# .github/scripts/universal_line_analyzer.py
import os
import openai
from github import Github
from typing import List, Dict
from universal_code_analyzer import UniversalCodeAnalyzer

class UniversalLineAnalyzer:
    def __init__(self):
        self.openai_client = openai.OpenAI(api_key=os.environ['OPENAI_API_KEY'])
        self.github_client = Github(os.environ['GITHUB_TOKEN'])
        self.repo_name = os.environ['REPO_NAME']
        self.pr_number = int(os.environ['PR_NUMBER'])

        self.repo = self.github_client.get_repo(self.repo_name)
        self.pr = self.repo.get_pull(self.pr_number)

        # 범용 코드 분석기 초기화
        self.universal_analyzer = UniversalCodeAnalyzer(self.repo, self.pr)

    def read_conventions(self):
        """README에서 컨벤션 정보 읽기"""
        try:
            readme = self.repo.get_contents("README.md")
            readme_content = readme.decoded_content.decode('utf-8')

            import re
            convention_match = re.search(
                r'## AI 리뷰 가이드라인.*?(?=##|$)',
                readme_content,
                re.DOTALL | re.IGNORECASE
            )

            if convention_match:
                return convention_match.group(0)
            else:
                return "컨벤션 가이드라인이 README에서 발견되지 않았습니다."

        except Exception as e:
            print(f"README 읽기 실패: {e}")
            return "컨벤션 정보를 읽을 수 없습니다."

    def analyze_file_for_issues(self, file_path: str, file_content: str, patch: str, conventions: str) -> List[Dict]:
        """파일 분석 (정적 분석 + AI 분석)"""

        language = self.universal_analyzer.detect_language(file_path)
        if not language:
            print(f"  ⚠️ 지원하지 않는 파일 형식: {file_path}")
            return []

        all_issues = []

        # 1. 정적 분석 (언어별 린터)
        static_issues = self.universal_analyzer.analyze_file(file_path, file_content)
        all_issues.extend(static_issues)

        # 2. AI 기반 고급 분석
        ai_issues = self.analyze_with_ai_advanced(file_path, file_content, patch, conventions, language)
        all_issues.extend(ai_issues)

        return all_issues

    def analyze_with_ai_advanced(self, file_path: str, file_content: str, patch: str, conventions: str, language: str) -> List[Dict]:
        """AI 기반 고급 코드 품질 분석"""

        # 언어별 특화 분석 포인트
        language_specific_points = {
            'kotlin': [
                "Android 메모리 누수 (Handler, Listener 등)",
                "코루틴 스코프 관리",
                "Room 데이터베이스 쿼리 최적화",
                "Compose 리컴포지션 최적화"
            ],
            'swift': [
                "iOS 메모리 누수 (강한 순환 참조)",
                "DispatchQueue 사용 최적화",
                "Core Data 성능 문제",
                "UIKit 생명주기 관리"
            ],
            'javascript': [
                "메모리 누수 (이벤트 리스너, 클로저)",
                "비동기 처리 최적화",
                "DOM 조작 성능",
                "번들 크기 최적화"
            ]
        }

        specific_points = language_specific_points.get(language, [])

        analysis_prompt = f"""
당신은 {language} 코드 리뷰 전문가입니다. 정적 분석 도구로는 찾기 어려운 고급 문제점을 분석해주세요.

**파일:** {file_path}
**언어:** {language}
**팀 컨벤션:** {conventions}

**{language} 특화 분석 포인트:**
{chr(10).join(f'- {point}' for point in specific_points)}

**파일 내용 (일부):**
```{language}
{file_content[:2000]}
```

**변경사항:**
```diff
{patch[:1500]}
```

**분석 대상:**

**P2 (중간 우선순위):**
- 메모리 누수 위험
- 성능 이슈 (O(n²) 알고리즘, 불필요한 연산)
- 안티패턴 (God Object, 강한 결합)
- 동시성/비동기 처리 문제
- 보안 취약점

**P3 (낮은 우선순위):**
- 복잡한 로직 (순환 복잡도 높음)
- 코드 중복
- 매직 넘버/스트링
- 과도한 매개변수
- 네이밍 개선 여지

**응답 형식:**
```json
[
  {{
    "line": 줄번호,
    "priority": "P2"|"P3",
    "category": "메모리|성능|안티패턴|동시성|보안|복잡도|중복|네이밍",
    "message": "구체적인 문제와 {language} 특화 개선방안",
    "suggestion": "개선된 코드 예시"
  }}
]
```

변경된 부분만 분석하고, 실제 문제가 있을 때만 보고해주세요.
"""

        try:
            response = self.openai_client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": f"{language} 전문 코드 리뷰어로서 정적 분석 도구가 놓치는 고급 문제를 찾아냅니다."},
                    {"role": "user", "content": analysis_prompt}
                ],
                max_tokens=1200,
                temperature=0.1
            )

            response_text = response.choices[0].message.content.strip()

            import json
            try:
                issues = json.loads(response_text)
                return issues if isinstance(issues, list) else []
            except json.JSONDecodeError:
                print(f"AI 분석 JSON 파싱 실패: {response_text[:200]}")
                return []

        except Exception as e:
            print(f"AI 분석 실패: {e}")
            return []

    def create_review_comments(self, all_issues: Dict[str, List[Dict]]):
        """GitHub Review API로 라인별 코멘트 생성"""

        if not any(all_issues.values()):
            print("발견된 이슈가 없습니다.")
            return

        comments = []
        linter_counts = {}  # 린터별 이슈 개수
        ai_count = 0

        for file_path, issues in all_issues.items():
            language = self.universal_analyzer.detect_language(file_path)

            for issue in issues:
                # 이슈 출처 구분
                category = issue.get('category', 'unknown')

                if category in ['ktlint', 'swiftlint', 'eslint']:
                    linter_counts[category] = linter_counts.get(category, 0) + 1
                    source_emoji = '🔧'
                    source_text = category
                else:
                    ai_count += 1
                    source_emoji = '🤖'
                    source_text = 'AI 분석'

                # 우선순위별 이모지
                priority_emoji = {'P2': '🟡', 'P3': '🔵'}

                comment_body = f"{priority_emoji.get(issue['priority'], '📝')} **[{issue['priority']}]** {source_emoji} **{source_text}**\n\n"
                comment_body += f"**{issue['category']}**: {issue['message']}\n"

                if issue.get('suggestion'):
                    comment_body += f"\n**💡 개선 제안:**\n```{language}\n{issue['suggestion']}\n```"

                comments.append({
                    'path': file_path,
                    'line': issue['line'],
                    'body': comment_body
                })

        # GitHub Review 생성
        try:
            review_body = f"🤖 **범용 코드 품질 자동 검수 결과**\n\n"

            # 린터별 이슈 개수 표시
            for linter, count in linter_counts.items():
                review_body += f"🔧 **{linter}**: {count}개 이슈\n"

            if ai_count > 0:
                review_body += f"🤖 **AI 고급 분석**: {ai_count}개 이슈\n"

            review_body += f"\n**지원 언어:** {', '.join(self.universal_analyzer.linters.keys())}\n"
            review_body += "검토 후 필요시 수정해주세요. 정적 분석 이슈는 IDE에서 자동 수정 가능합니다."

            review = self.pr.create_review(
                body=review_body,
                event="COMMENT",
                comments=comments
            )

            total_static = sum(linter_counts.values())
            print(f"✅ 총 {len(comments)}개 코멘트 (정적분석: {total_static}, AI: {ai_count})가 포함된 리뷰가 생성되었습니다: {review.html_url}")

        except Exception as e