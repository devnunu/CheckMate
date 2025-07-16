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

    def parse_diff_for_changed_lines(self, patch: str) -> Dict[int, int]:
        """diff patch를 파싱하여 실제 변경된 라인 번호와 diff position 매핑"""
        if not patch:
            return {}

        line_mapping = {}  # {실제_파일_라인: diff_position}
        current_file_line = 0
        diff_position = 0

        lines = patch.split('\n')

        for line in lines:
            if line.startswith('@@'):
                # @@ -old_start,old_count +new_start,new_count @@ 형식 파싱
                match = re.search(r'\+(\d+)', line)
                if match:
                    current_file_line = int(match.group(1)) - 1
            elif line.startswith('+'):
                # 추가된 라인
                current_file_line += 1
                line_mapping[current_file_line] = diff_position
            elif line.startswith(' '):
                # 변경되지 않은 라인 (컨텍스트)
                current_file_line += 1
                line_mapping[current_file_line] = diff_position
            # '-'로 시작하는 라인은 삭제된 라인이므로 current_file_line 증가하지 않음

            diff_position += 1

        return line_mapping

    def get_changed_lines_only(self, file_path: str, patch: str) -> List[int]:
        """실제로 변경된 라인 번호만 추출 (+ 라인)"""
        if not patch:
            return []

        changed_lines = []
        current_file_line = 0

        lines = patch.split('\n')

        for line in lines:
            if line.startswith('@@'):
                # @@ -old_start,old_count +new_start,new_count @@ 형식 파싱
                match = re.search(r'\+(\d+)', line)
                if match:
                    current_file_line = int(match.group(1)) - 1
            elif line.startswith('+'):
                # 추가된 라인만 분석 대상
                current_file_line += 1
                changed_lines.append(current_file_line)
            elif line.startswith(' '):
                # 컨텍스트 라인
                current_file_line += 1
            # '-' 라인은 무시

        return changed_lines

    def analyze_file_for_issues(self, file_path: str, file_content: str, patch: str) -> List[Dict]:
        """파일 분석 - 변경된 라인만 대상으로"""

        language = self.universal_analyzer.detect_language(file_path)
        if not language:
            print(f"  ⚠️ 지원하지 않는 파일 형식: {file_path}")
            return []

        # 실제 변경된 라인만 가져오기
        changed_lines = self.get_changed_lines_only(file_path, patch)
        if not changed_lines:
            print(f"  ⚠️ 변경된 라인이 없음: {file_path}")
            return []

        print(f"  📝 {file_path}: {len(changed_lines)}개 라인 변경됨")

        # 변경된 라인 주변의 컨텍스트 추출
        file_lines = file_content.split('\n')
        analysis_chunks = []

        for line_num in changed_lines:
            # 변경된 라인 주변 ±3라인 컨텍스트 포함
            start_line = max(1, line_num - 3)
            end_line = min(len(file_lines), line_num + 3)

            chunk_lines = []
            for i in range(start_line - 1, end_line):  # 0-based index
                if i < len(file_lines):
                    prefix = ">>>" if (i + 1) == line_num else "   "  # 변경 라인 표시
                    chunk_lines.append(f"{prefix} {i + 1:3d}: {file_lines[i]}")

            analysis_chunks.append({
                'target_line': line_num,
                'context': '\n'.join(chunk_lines)
            })

        # AI 분석 실행
        all_issues = []
        for chunk in analysis_chunks:
            issues = self.analyze_chunk_with_ai(file_path, chunk, language)
            all_issues.extend(issues)

        return all_issues

    def analyze_chunk_with_ai(self, file_path: str, chunk: Dict, language: str) -> List[Dict]:
        """AI 기반 코드 청크 분석"""

        analysis_prompt = f"""
파일: {file_path} (언어: {language})
변경된 라인: {chunk['target_line']}

코드 컨텍스트:
```
{chunk['context']}
```

>>> 표시된 라인({chunk['target_line']})이 새로 추가되거나 수정된 코드입니다.

다음 관점에서 분석해주세요:
1. **네이밍**: 변수명, 함수명이 명확하고 일관적인가?
2. **로직 오류**: 조건문, 반복문에서 예상과 다른 동작 가능성
3. **널 안전성**: null 체크 누락, 옵셔널 처리 미흡
4. **메모리 관리**: 리소스 해제, 강한 참조 순환
5. **성능**: 비효율적인 연산, 불필요한 객체 생성
6. **에러 처리**: 예외 상황 대응 부족

JSON 형식으로만 응답 (마크다운 코드블록 없이):
[
  {{
    "line": {chunk['target_line']},
    "priority": "P2"|"P3",
    "category": "네이밍|로직|널안전성|메모리|성능|에러처리",
    "message": "문제점을 50자 이내로",
    "suggestion": "개선 방안을 한 줄로"
  }}
]

문제가 없으면 빈 배열 []을 반환하세요.
실제 문제가 있을 때만 포함하고, 변경된 라인과 직접 관련된 이슈만 지적하세요.
"""

        try:
            response = self.openai_client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": f"순수 JSON만 응답하는 {language} 코드 분석 전문가입니다. 변경된 라인만 집중 분석하세요."},
                    {"role": "user", "content": analysis_prompt}
                ],
                max_tokens=800,
                temperature=0.1
            )

            response_text = response.choices[0].message.content.strip()
            response_text = self.clean_json_response(response_text)

            import json
            try:
                issues = json.loads(response_text)
                return issues if isinstance(issues, list) else []
            except json.JSONDecodeError as e:
                print(f"AI 분석 JSON 파싱 실패: {response_text[:200]}...")
                return []

        except Exception as e:
            print(f"AI 분석 실패: {e}")
            return []

    def clean_json_response(self, response_text: str) -> str:
        """AI 응답에서 순수 JSON만 추출"""
        # 마크다운 코드 블록 제거
        if "```json" in response_text:
            start = response_text.find("```json") + 7
            end = response_text.find("```", start)
            if end != -1:
                response_text = response_text[start:end].strip()
            else:
                response_text = response_text[start:].strip()
        elif "```" in response_text:
            start = response_text.find("```") + 3
            end = response_text.find("```", start)
            if end != -1:
                response_text = response_text[start:end].strip()

        # 앞뒤 불필요한 텍스트 제거
        response_text = response_text.strip()

        # JSON 배열이 시작하는 지점 찾기
        start_bracket = response_text.find('[')
        if start_bracket != -1:
            response_text = response_text[start_bracket:]

        # JSON 배열이 끝나는 지점 찾기 (마지막 ]까지)
        end_bracket = response_text.rfind(']')
        if end_bracket != -1:
            response_text = response_text[:end_bracket + 1]

        return response_text

    def create_review_comments(self, all_issues: Dict[str, List[Dict]]):
        """GitHub Review API로 라인별 코멘트 생성 (정확한 라인 매핑)"""

        if not any(all_issues.values()):
            print("발견된 이슈가 없습니다.")
            return

        comments = []
        total_comments = 0

        for file_path, issues in all_issues.items():
            if not issues:
                continue

            # 해당 파일의 diff 정보 가져오기
            pr_file = None
            for file in self.pr.get_files():
                if file.filename == file_path:
                    pr_file = file
                    break

            if not pr_file or not pr_file.patch:
                print(f"⚠️ {file_path}: diff 정보 없음, 코멘트 건너뛰기")
                continue

            # diff 라인 매핑 생성
            line_mapping = self.parse_diff_for_changed_lines(pr_file.patch)
            language = self.universal_analyzer.detect_language(file_path)

            for issue in issues:
                file_line = issue['line']

                # 해당 라인이 실제로 변경된 라인인지 확인
                if file_line not in line_mapping:
                    print(f"⚠️ {file_path}:{file_line} - 변경되지 않은 라인, 코멘트 건너뛰기")
                    continue

                diff_position = line_mapping[file_line]

                # 우선순위별 이모지
                priority_emoji = {'P2': '🟡', 'P3': '🔵'}

                comment_body = f"{priority_emoji.get(issue['priority'], '📝')} **[{issue['priority']}] AI 분석**\n\n"
                comment_body += f"**{issue['category']}**: {issue['message']}\n"

                if issue.get('suggestion'):
                    comment_body += f"\n**💡 개선 제안:**\n```{language}\n{issue['suggestion']}\n```"

                # GitHub Review API 코멘트 형식 (position 기반)
                comments.append({
                    'path': file_path,
                    'body': comment_body,
                    'position': diff_position  # diff 내 위치 사용
                })
                total_comments += 1

        if not comments:
            print("⚠️ 생성할 수 있는 코멘트가 없습니다.")
            return

        # GitHub Review 생성
        try:
            review = self.pr.create_review(
                event="COMMENT",
                comments=comments
            )

            print(f"✅ 총 {total_comments}개 라인별 코멘트가 정확한 위치에 생성되었습니다: {review.html_url}")

        except Exception as e:
            print(f"❌ 라인별 리뷰 생성 실패: {e}")
            print("🔧 대체 방법으로 일반 코멘트 생성...")
            self.create_fallback_comment(all_issues)

    def create_fallback_comment(self, all_issues: Dict[str, List[Dict]]):
        """Review API 실패 시 일반 코멘트로 대체"""
        comment_body = "🤖 **AI 코드 분석 결과**\n\n"

        for file_path, issues in all_issues.items():
            if issues:
                language = self.universal_analyzer.detect_language(file_path)
                comment_body += f"\n### 📁 {file_path} ({language})\n"

                for issue in issues:
                    priority_emoji = {'P2': '🟡', 'P3': '🔵'}
                    comment_body += f"- **Line {issue['line']}** {priority_emoji.get(issue['priority'], '📝')} [{issue['priority']}] {issue['category']}: {issue['message']}\n"

        try:
            self.pr.create_issue_comment(comment_body)
            print("✅ 대체 코멘트가 생성되었습니다.")
        except Exception as e:
            print(f"❌ 대체 코멘트 생성도 실패: {e}")

    def run_universal_analysis(self):
        """범용 분석 전체 프로세스 실행"""
        print("🔍 범용 코드 품질 검수를 시작합니다...")

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

                # 변경된 라인만 분석
                issues = self.analyze_file_for_issues(
                    file.filename,
                    file_content,
                    file.patch or ""
                )

                if issues:
                    all_issues[file.filename] = issues
                    print(f"  ⚠️ {len(issues)}개 이슈 발견")
                else:
                    print(f"  ✅ 이슈 없음")

            except Exception as e:
                print(f"  ❌ 분석 실패: {e}")
                continue

        # 결과 요약
        print(f"\n📊 분석 완료: {analyzed_count}개 파일 분석, {skipped_count}개 파일 건너뛰기")

        # 라인별 코멘트 생성
        if all_issues:
            total_issues = sum(len(issues) for issues in all_issues.values())
            print(f"📈 총 {total_issues}개 이슈 발견")
            self.create_review_comments(all_issues)
        else:
            print("✅ 모든 분석 대상 파일이 품질 기준을 통과했습니다!")

if __name__ == "__main__":
    analyzer = UniversalLineAnalyzer()
    analyzer.run_universal_analysis()