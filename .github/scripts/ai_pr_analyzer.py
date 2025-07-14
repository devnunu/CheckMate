# .github/scripts/ai_pr_analyzer.py
import os
import json
import openai
from github import Github
import re

class PRAnalyzer:
    def __init__(self):
        self.openai_client = openai.OpenAI(api_key=os.environ['OPENAI_API_KEY'])
        self.github_client = Github(os.environ['GITHUB_TOKEN'])
        self.repo_name = os.environ['REPO_NAME']
        self.pr_number = int(os.environ['PR_NUMBER'])
        self.pr_title = os.environ.get('PR_TITLE', '')
        self.pr_body = os.environ.get('PR_BODY', '')

        # GitHub repo 객체
        self.repo = self.github_client.get_repo(self.repo_name)
        self.pr = self.repo.get_pull(self.pr_number)

    def read_conventions(self):
        """README에서 컨벤션 정보 읽기"""
        try:
            readme = self.repo.get_contents("README.md")
            readme_content = readme.decoded_content.decode('utf-8')

            # AI 리뷰 가이드라인 섹션 추출
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

    def get_changed_files_content(self):
        """변경된 파일들의 내용과 diff 정보 가져오기"""
        changed_files = []

        # PR의 모든 파일 변경사항 가져오기
        files = self.pr.get_files()

        for file in files:
            file_info = {
                'filename': file.filename,
                'status': file.status,  # added, modified, removed
                'additions': file.additions,
                'deletions': file.deletions,
                'patch': file.patch if hasattr(file, 'patch') else None,
                'content': None
            }

            # 삭제된 파일이 아닌 경우 현재 내용도 가져오기
            if file.status != 'removed':
                try:
                    content = self.repo.get_contents(file.filename, ref=self.pr.head.sha)
                    file_info['content'] = content.decoded_content.decode('utf-8')
                except:
                    file_info['content'] = "파일 내용을 읽을 수 없습니다."

            changed_files.append(file_info)

        return changed_files

    def analyze_with_ai(self, changed_files, conventions):
        """AI를 사용하여 PR 분석"""

        # 분석할 내용 준비
        analysis_prompt = f"""
당신은 코드 리뷰 전문가입니다. 다음 PR을 분석하여 리뷰 템플릿을 생성해주세요.

**PR 정보:**
- 제목: {self.pr_title}
- 설명: {self.pr_body}

**팀 컨벤션:**
{conventions}

**변경된 파일들:**
"""

        # 각 파일의 변경사항 추가
        for file in changed_files:
            analysis_prompt += f"\n### {file['filename']} ({file['status']})\n"
            analysis_prompt += f"추가: {file['additions']}줄, 삭제: {file['deletions']}줄\n"

            if file['patch']:
                # diff가 너무 길면 자르기 (토큰 제한 고려)
                patch = file['patch'][:3000] if len(file['patch']) > 3000 else file['patch']
                analysis_prompt += f"```diff\n{patch}\n```\n"

        analysis_prompt += """

**요청사항:**
다음 형식으로 PR 분석 결과를 작성해주세요:

## 🤖 AI PR 분석 결과

### 📋 작업 개요
[전체적인 변경사항의 목적과 의도를 요약해주세요]

### 🔧 주요 변경사항
[파일별/기능별 주요 변경사항을 요약해주세요]

### ⚠️ 리뷰 집중 포인트
[다음 태그를 사용하여 위험 가능성이 있는 부분을 표시해주세요]
- 🔴 **[로직오류위험]** `파일명:라인` - 설명
- 🟡 **[사이드이펙트]** `파일명:라인` - 설명
- 🔵 **[성능저하]** `파일명:라인` - 설명
- 🟠 **[보안취약]** `파일명:라인` - 설명
- 🟣 **[호환성이슈]** `파일명:라인` - 설명
- ⚫ **[데이터정합성]** `파일명:라인` - 설명

### 💡 추가 권장사항
[코드 품질 향상을 위한 일반적인 제안사항이 있다면 포함해주세요]

중요하거나 위험할 가능성이 있는 변경사항만 선별해서 포함해주세요.
"""

        try:
            response = self.openai_client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": "당신은 안드로이드/iOS 개발에 전문적인 코드 리뷰어입니다. PR의 전체적인 맥락을 파악하여 실수 가능성이 높은 부분을 중심으로 정확하고 실용적인 분석을 제공합니다."},
                    {"role": "user", "content": analysis_prompt}
                ],
                max_tokens=2500,
                temperature=0.3
            )

            return response.choices[0].message.content

        except Exception as e:
            return f"❌ AI PR 분석 중 오류가 발생했습니다: {str(e)}"

    def minimize_previous_comments(self):
        """이전 AI 분석 코멘트를 minimize 처리"""
        comments = self.pr.get_issue_comments()

        for comment in comments:
            # AI 봇이 작성한 코멘트 찾기
            if (comment.user.login == 'github-actions[bot]' and
                '🤖 AI PR 분석 결과' in comment.body):

                # 이전 코멘트 삭제 (또는 minimize 처리)
                try:
                    comment.delete()  # 완전 삭제
                    print(f"이전 분석 코멘트 삭제: {comment.id}")
                except Exception as e:
                    # 삭제 권한이 없는 경우 minimize 처리
                    try:
                        updated_body = f"<!-- Minimized by new analysis -->\n<details>\n<summary>이전 분석 결과 (클릭하여 보기)</summary>\n\n{comment.body}\n</details>"
                        comment.edit(updated_body)
                        print(f"이전 코멘트 minimize 처리: {comment.id}")
                    except Exception as e2:
                        print(f"코멘트 처리 실패: {e2}")

    def post_analysis_comment(self, analysis_result):
        """분석 결과를 PR에 코멘트로 등록"""

        # 이전 PR 분석 코멘트들을 삭제/minimize 처리
        self.minimize_previous_comments()

        # PR 분석 템플릿 코멘트 등록
        try:
            comment = self.pr.create_issue_comment(analysis_result)
            print(f"✅ PR 분석 템플릿 코멘트 등록 완료: {comment.html_url}")
            return True
        except Exception as e:
            print(f"❌ PR 템플릿 코멘트 등록 실패: {e}")
            return False

    def run_analysis(self):
        """전체 분석 프로세스 실행"""
        print("🚀 AI PR 분석을 시작합니다...")

        # 1. 컨벤션 정보 읽기
        print("📖 컨벤션 정보를 읽는 중...")
        conventions = self.read_conventions()

        # 2. 변경된 파일 정보 가져오기
        print("📁 변경된 파일 정보를 수집하는 중...")
        changed_files = self.get_changed_files_content()

        if not changed_files:
            print("❌ 변경된 파일이 없습니다.")
            return

        print(f"📊 총 {len(changed_files)}개 파일이 변경되었습니다.")

        # 3. AI 분석 실행
        print("🤖 AI 분석을 실행하는 중...")
        analysis_result = self.analyze_with_ai(changed_files, conventions)

        # 4. 결과를 PR에 코멘트로 등록
        print("💬 분석 결과를 PR에 등록하는 중...")
        success = self.post_analysis_comment(analysis_result)

        if success:
            print("✅ AI PR 분석이 완료되었습니다!")
        else:
            print("❌ 분석 결과 등록에 실패했습니다.")

if __name__ == "__main__":
    analyzer = PRAnalyzer()
    analyzer.run_analysis()