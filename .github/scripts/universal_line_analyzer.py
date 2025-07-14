# .github/scripts/universal_line_analyzer.py
import os
import re
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
        """파일 분석 (AI 기반 린트 분석 + AI 고급 분석)"""

        language = self.universal_analyzer.detect_language(file_path)
        if not language:
            print(f"  ⚠️ 지원하지 않는 파일 형식: {file_path}")
            return []

        all_issues = []

        # 1. AI 기반 린트 분석 (설정 파일 기반)
        lint_issues = self.universal_analyzer.analyze_file(file_path, file_content)
        all_issues.extend(lint_issues)

        # 2. AI 기반 고급 분석 (메모리 누수, 안티패턴 등)
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

    def get_existing_review_comments(self):
        """기존 AI 리뷰 코멘트 분석"""
        existing_comments = {}

        try:
            # PR의 모든 리뷰 가져오기
            reviews = self.pr.get_reviews()

            for review in reviews:
                # github-actions bot이 작성한 리뷰만 확인
                if review.user.login == 'github-actions[bot]':
                    # 리뷰의 라인별 코멘트 가져오기
                    review_comments = self.pr.get_review_comments()

                    for comment in review_comments:
                        if comment.user.login == 'github-actions[bot]':
                            # 파일 경로와 라인 번호를 키로 사용
                            key = f"{comment.path}:{comment.line}"

                            # 코멘트 내용에서 핵심 부분 추출 (우선순위, 카테고리, 메시지)
                            comment_info = self.extract_comment_info(comment.body)
                            existing_comments[key] = comment_info

            return existing_comments

        except Exception as e:
            print(f"기존 코멘트 조회 실패: {e}")
            return {}

    def extract_comment_info(self, comment_body: str):
        """코멘트에서 핵심 정보 추출"""
        try:
            import re

            # 우선순위 추출 [P2] 또는 [P3]
            priority_match = re.search(r'\[P([23])\]', comment_body)
            priority = f"P{priority_match.group(1)}" if priority_match else "P3"

            # 카테고리 추출 **카테고리**: 메시지
            category_match = re.search(r'\*\*([^*]+)\*\*:\s*([^\n]+)', comment_body)
            category = category_match.group(1).strip() if category_match else "unknown"
            message = category_match.group(2).strip() if category_match else ""

            return {
                'priority': priority,
                'category': category,
                'message': message,
                'full_body': comment_body
            }

        except Exception:
            return {
                'priority': 'P3',
                'category': 'unknown',
                'message': '',
                'full_body': comment_body
            }

    def is_duplicate_comment(self, new_issue: Dict, existing_comment: Dict) -> bool:
        """새 이슈가 기존 코멘트와 중복인지 확인"""

        # 우선순위와 카테고리가 같은지 확인
        if (new_issue.get('priority') == existing_comment.get('priority') and
            new_issue.get('category') == existing_comment.get('category')):

            # 메시지 유사도 확인 (간단한 키워드 기반)
            new_message = new_issue.get('message', '').lower()
            existing_message = existing_comment.get('message', '').lower()

            # 핵심 키워드가 포함되어 있는지 확인
            key_phrases = [
                '들여쓰기', '4칸 스페이스', 'camelcase', '함수명', '변수명',
                '메모리 누수', '성능', '안티패턴', '복잡도', '네이밍'
            ]

            for phrase in key_phrases:
                if phrase in new_message and phrase in existing_message:
                    return True

            # 메시지가 70% 이상 유사하면 중복으로 판단
            similarity = self.calculate_message_similarity(new_message, existing_message)
            if similarity > 0.7:
                return True

        return False

    def calculate_message_similarity(self, message1: str, message2: str) -> float:
        """두 메시지의 유사도 계산 (간단한 단어 기반)"""
        if not message1 or not message2:
            return 0.0

        words1 = set(message1.split())
        words2 = set(message2.split())

        if not words1 or not words2:
            return 0.0

        intersection = words1.intersection(words2)
        union = words1.union(words2)

        return len(intersection) / len(union)

    def filter_duplicate_issues(self, all_issues: Dict[str, List[Dict]]) -> Dict[str, List[Dict]]:
        """중복 이슈 필터링"""

        print("🔍 기존 코멘트와 중복 여부를 확인하는 중...")

        # 기존 코멘트 가져오기
        existing_comments = self.get_existing_review_comments()

        if not existing_comments:
            print("  ✅ 기존 AI 코멘트가 없음 - 모든 이슈 분석")
            return all_issues

        filtered_issues = {}
        total_issues = 0
        duplicate_count = 0

        for file_path, issues in all_issues.items():
            filtered_file_issues = []

            for issue in issues:
                total_issues += 1
                line = issue['line']
                comment_key = f"{file_path}:{line}"

                # 해당 라인에 기존 코멘트가 있는지 확인
                if comment_key in existing_comments:
                    existing_comment = existing_comments[comment_key]

                    # 중복 여부 확인
                    if self.is_duplicate_comment(issue, existing_comment):
                        duplicate_count += 1
                        print(f"  ⏭️ 중복 건너뛰기: {file_path}:{line} - {issue['category']}")
                        continue
                    else:
                        print(f"  🔄 다른 이슈 감지: {file_path}:{line} - {issue['category']}")

                filtered_file_issues.append(issue)

            if filtered_file_issues:
                filtered_issues[file_path] = filtered_file_issues

        print(f"📊 중복 필터링 결과: 전체 {total_issues}개 중 {duplicate_count}개 중복 제거")
        return filtered_issues

    def create_review_comments(self, all_issues: Dict[str, List[Dict]]):
        """GitHub Review API로 라인별 코멘트 생성 (중복 방지 포함)"""

        if not any(all_issues.values()):
            print("발견된 이슈가 없습니다.")
            return

        # 중복 이슈 필터링
        filtered_issues = self.filter_duplicate_issues(all_issues)

        if not any(filtered_issues.values()):
            print("✅ 모든 이슈가 기존 코멘트와 중복됩니다. 새로운 코멘트를 생성하지 않습니다.")
            return

        comments = []
        linter_counts = {}  # 린터별 이슈 개수
        advanced_count = 0

        for file_path, issues in filtered_issues.items():
            language = self.universal_analyzer.detect_language(file_path)

            for issue in issues:
                # 이슈 출처 구분
                category = issue.get('category', 'unknown')

                if category in ['kotlinlint', 'swiftlint', 'eslint']:
                    linter_counts[category] = linter_counts.get(category, 0) + 1
                    source_emoji = '🤖'
                    source_text = f'AI {category}'
                else:
                    advanced_count += 1
                    source_emoji = '🧠'
                    source_text = 'AI 고급분석'

                # 우선순위별 이모지
                priority_emoji = {'P2': '🟡', 'P3': '🔵'}

                comment_body = f"{priority_emoji.get(issue['priority'], '📝')} **[{issue['priority']}] {source_text}**\n\n"
                comment_body += f"**{issue['category']}**: {issue['message']}\n"

                if issue.get('suggestion'):
                    comment_body += f"\n**💡 개선 제안:**\n```{language}\n{issue['suggestion']}\n```"

                # GitHub Review API 코멘트 형식
                comments.append({
                    'path': file_path,
                    'body': comment_body,
                    'line': issue['line'],
                    'side': 'RIGHT'  # 변경된 코드 라인에 코멘트 (RIGHT = 새 버전, LEFT = 이전 버전)
                })

        # GitHub Review 생성 (요약 코멘트 없이 라인별 코멘트만)
        try:
            # Review 생성 (body 없이 라인별 코멘트만)
            review = self.pr.create_review(
                event="COMMENT",
                comments=comments
            )

            total_lint = sum(linter_counts.values())
            print(f"✅ 총 {len(comments)}개 새로운 라인별 코멘트 (AI 린트: {total_lint}, 고급분석: {advanced_count})가 생성되었습니다: {review.html_url}")

        except Exception as e:
            print(f"❌ 라인별 리뷰 생성 실패: {e}")
            print("Diff 기반 라인별 코멘트로 재시도...")
            self.create_diff_based_comments(filtered_issues)

    def create_diff_based_comments(self, filtered_issues: Dict[str, List[Dict]]):
        """Diff 기반 라인별 코멘트 생성 (대체 방법)"""

        comments = []

        for file_path, issues in filtered_issues.items():
            # PR에서 해당 파일의 diff 정보 가져오기
            pr_file = None
            for file in self.pr.get_files():
                if file.filename == file_path:
                    pr_file = file
                    break

            if not pr_file or not pr_file.patch:
                continue

            # diff에서 변경된 라인 번호 매핑
            diff_line_mapping = self.parse_diff_line_mapping(pr_file.patch)

            language = self.universal_analyzer.detect_language(file_path)

            for issue in issues:
                file_line = issue['line']

                # 실제 파일 라인을 diff 라인으로 변환
                diff_line = self.convert_file_line_to_diff_line(file_line, diff_line_mapping)

                if diff_line is None:
                    continue  # 변경되지 않은 라인은 코멘트 불가

                category = issue.get('category', 'unknown')
                if category in ['ktlint', 'swiftlint', 'eslint']:
                    source_emoji = '🔧'
                    source_text = category
                else:
                    source_emoji = '🤖'
                    source_text = 'AI 분석'

                priority_emoji = {'P2': '🟡', 'P3': '🔵'}

                comment_body = f"{priority_emoji.get(issue['priority'], '📝')} **[{issue['priority']}] {source_text}**\n\n"
                comment_body += f"**{issue['category']}**: {issue['message']}\n"

                if issue.get('suggestion'):
                    comment_body += f"\n**💡 개선 제안:**\n```{language}\n{issue['suggestion']}\n```"

                comments.append({
                    'path': file_path,
                    'body': comment_body,
                    'position': diff_line  # diff 내에서의 위치
                })

        # Review 생성 (요약 코멘트 없이)
        try:
            review = self.pr.create_review(
                event="COMMENT",
                comments=comments
            )

            print(f"✅ {len(comments)}개 diff 기반 새로운 라인별 코멘트가 생성되었습니다: {review.html_url}")

        except Exception as e:
            print(f"❌ Diff 기반 코멘트 생성도 실패: {e}")
            self.create_fallback_comment(filtered_issues)

    def parse_diff_line_mapping(self, patch: str) -> Dict[int, int]:
        """diff patch에서 파일 라인 번호 → diff 위치 매핑 생성"""
        mapping = {}
        lines = patch.split('\n')

        current_new_line = 0
        diff_position = 0

        for line in lines:
            if line.startswith('@@'):
                # @@ -old_start,old_count +new_start,new_count @@ 형식 파싱
                import re
                match = re.search(r'\+(\d+)', line)
                if match:
                    current_new_line = int(match.group(1)) - 1
            elif line.startswith('+'):
                current_new_line += 1
                mapping[current_new_line] = diff_position
            elif line.startswith(' '):
                current_new_line += 1
                mapping[current_new_line] = diff_position
            # '-'로 시작하는 라인은 current_new_line 증가하지 않음

            diff_position += 1

        return mapping

    def convert_file_line_to_diff_line(self, file_line: int, mapping: Dict[int, int]) -> int:
        """파일 라인 번호를 diff 위치로 변환"""
        return mapping.get(file_line)

    def create_fallback_comment(self, filtered_issues: Dict[str, List[Dict]]):
        """Review API 실패 시 일반 코멘트로 대체"""
        comment_body = "🤖 **범용 코드 품질 검수 결과**\n\n"

        for file_path, issues in filtered_issues.items():
            if issues:
                language = self.universal_analyzer.detect_language(file_path)
                comment_body += f"\n### 📁 {file_path} ({language})\n"

                for issue in issues:
                    category = issue.get('category', 'unknown')
                    if category in ['ktlint', 'swiftlint', 'eslint']:
                        source_emoji = '🔧'
                    else:
                        source_emoji = '🤖'

                    priority_emoji = {'P2': '🟡', 'P3': '🔵'}
                    comment_body += f"- **Line {issue['line']}** {priority_emoji.get(issue['priority'], '📝')} [{issue['priority']}] {source_emoji} {issue['category']}: {issue['message']}\n"

        try:
            self.pr.create_issue_comment(comment_body)
            print("✅ 대체 코멘트가 생성되었습니다.")
        except Exception as e:
            print(f"❌ 대체 코멘트 생성도 실패: {e}")

    def run_universal_analysis(self):
        """범용 분석 전체 프로세스 실행"""
        print("🔍 범용 코드 품질 검수를 시작합니다...")

        # 분석 설정 요약
        analysis_summary = self.universal_analyzer.get_analysis_summary()
        print(f"📋 {analysis_summary}")

        # 컨벤션 정보 읽기
        conventions = self.read_conventions()

        # 지원하는 파일 확장자
        supported_extensions = self.universal_analyzer.get_supported_extensions()

        # PR의 변경된 파일들 가져오기
        files = self.pr.get_files()
        all_issues = {}
        analyzed_count = 0
        skipped_count = 0

        for file in files:
            # 삭제된 파일 건너뛰기
            if file.status == 'removed':
                continue

            # 지원하는 파일 확장자 확인
            is_supported = any(file.filename.endswith(ext) for ext in supported_extensions)
            if not is_supported:
                skipped_count += 1
                continue

            print(f"📝 분석 중: {file.filename}")
            analyzed_count += 1

            try:
                # 현재 파일 내용 가져오기
                content = self.repo.get_contents(file.filename, ref=self.pr.head.sha)
                file_content = content.decoded_content.decode('utf-8')

                # 파일별 이슈 분석
                issues = self.analyze_file_for_issues(
                    file.filename,
                    file_content,
                    file.patch or "",
                    conventions
                )

                if issues:
                    all_issues[file.filename] = issues

                    # 이슈 분류별 개수 계산
                    lint_issues = [i for i in issues if i.get('category') in ['kotlinlint', 'swiftlint', 'eslint']]
                    advanced_issues = [i for i in issues if i.get('category') not in ['kotlinlint', 'swiftlint', 'eslint']]

                    print(f"  ⚠️ 총 {len(issues)}개 이슈 (린트: {len(lint_issues)}, 고급분석: {len(advanced_issues)})")
                else:
                    print(f"  ✅ 이슈 없음")

            except Exception as e:
                print(f"  ❌ 분석 실패: {e}")
                continue

        # 결과 요약
        print(f"\n📊 분석 완료: {analyzed_count}개 파일 분석, {skipped_count}개 파일 건너뛰기")

        # 리뷰 코멘트 생성
        if all_issues:
            # 전체 이슈 통계
            total_static = 0
            total_ai = 0
            linter_stats = {}

            for issues in all_issues.values():
                for issue in issues:
                    category = issue.get('category', 'unknown')
                    if category in ['kotlinlint', 'swiftlint', 'eslint']:
                        total_lint += 1
                        linter_stats[category] = linter_stats.get(category, 0) + 1
                    else:
                        total_advanced += 1

            print(f"📈 검수 완료:")
            for linter, count in linter_stats.items():
                print(f"  🔧 {linter}: {count}개")
            if total_ai > 0:
                print(f"  🤖 AI 분석: {total_ai}개")

            self.create_review_comments(all_issues)
        else:
            print("✅ 모든 분석 대상 파일이 품질 기준을 통과했습니다!")

if __name__ == "__main__":
    analyzer = UniversalLineAnalyzer()
    analyzer.run_universal_analysis()