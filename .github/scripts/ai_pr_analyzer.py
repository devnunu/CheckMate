# .github/scripts/ai_pr_analyzer.py
import os
import json
import openai
from github import Github
import re
from datetime import datetime

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

    def get_project_context(self):
        """프로젝트 컨텍스트 파악 (언어, 프레임워크 등)"""
        try:
            # 프로젝트 파일들을 통해 기술 스택 파악
            tech_stack = []

            # 주요 설정 파일들 확인
            config_files = [
                'package.json', 'build.gradle', 'pom.xml', 'requirements.txt',
                'Podfile', 'Package.swift', 'go.mod', 'Cargo.toml'
            ]

            for config_file in config_files:
                try:
                    content = self.repo.get_contents(config_file)
                    if config_file == 'package.json':
                        tech_stack.append('JavaScript/Node.js')
                    elif config_file in ['build.gradle', 'pom.xml']:
                        tech_stack.append('Java/Android')
                    elif config_file == 'requirements.txt':
                        tech_stack.append('Python')
                    elif config_file in ['Podfile', 'Package.swift']:
                        tech_stack.append('iOS/Swift')
                    elif config_file == 'go.mod':
                        tech_stack.append('Go')
                    elif config_file == 'Cargo.toml':
                        tech_stack.append('Rust')
                except:
                    continue

            return ', '.join(tech_stack) if tech_stack else "General"

        except Exception as e:
            print(f"프로젝트 컨텍스트 파악 실패: {e}")
            return "General"

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

    def find_related_prs(self):
        """관련 PR들 찾기 (최근 30개 PR 중에서)"""
        try:
            # 최근 30개 PR 가져오기 (현재 PR 제외)
            recent_prs = list(self.repo.get_pulls(state='all', sort='updated', direction='desc'))[:30]
            related_prs = []

            # 현재 PR의 키워드 추출
            current_keywords = self.extract_keywords(self.pr_title + " " + (self.pr_body or ""))

            for pr in recent_prs:
                if pr.number == self.pr_number:  # 현재 PR 제외
                    continue

                # 각 PR의 키워드와 비교
                pr_keywords = self.extract_keywords(pr.title + " " + (pr.body or ""))

                # 공통 키워드 개수 계산
                common_keywords = current_keywords.intersection(pr_keywords)

                if len(common_keywords) >= 2:  # 2개 이상 공통 키워드
                    related_prs.append({
                        'number': pr.number,
                        'title': pr.title,
                        'state': pr.state,
                        'common_keywords': list(common_keywords)
                    })

            return related_prs[:3]  # 최대 3개만 반환

        except Exception as e:
            print(f"관련 PR 찾기 실패: {e}")
            return []

    def extract_keywords(self, text):
        """텍스트에서 키워드 추출"""
        # 간단한 키워드 추출 (개선 가능)
        keywords = set()

        # 일반적인 개발 관련 키워드들
        dev_keywords = [
            'fix', 'bug', 'feature', 'add', 'update', 'remove', 'refactor',
            'api', 'ui', 'test', 'security', 'performance', 'database',
            'auth', 'login', 'user', 'admin', 'config', 'lint', 'style'
        ]

        text_lower = text.lower()
        for keyword in dev_keywords:
            if keyword in text_lower:
                keywords.add(keyword)

        # 파일 확장자 추출
        extensions = re.findall(r'\.(\w+)', text)
        for ext in extensions:
            if ext in ['py', 'js', 'kt', 'swift', 'java', 'go', 'rs']:
                keywords.add(ext)

        return keywords

    def generate_walkthrough_summary(self, changed_files, project_context):
        """전문적인 Walkthrough 스타일의 PR Summary 생성"""

        # 변경된 파일들의 상세 정보 준비
        files_info = []
        for file in changed_files:
            file_summary = f"**{file['filename']}** ({file['status']})\n"
            file_summary += f"- 추가: {file['additions']}줄, 삭제: {file['deletions']}줄\n"

            if file['patch']:
                # diff 일부만 포함 (분석을 위해)
                patch_preview = file['patch'][:1000] if len(file['patch']) > 1000 else file['patch']
                file_summary += f"```diff\n{patch_preview}\n```\n"

            files_info.append(file_summary)

        analysis_prompt = f"""
당신은 코드 리뷰 전문가입니다. 전문적인 Walkthrough 스타일로 PR 분석을 수행해주세요.

**프로젝트 기술 스택:** {project_context}

**PR 정보:**
- 제목: {self.pr_title}
- 설명: {self.pr_body}

**변경된 파일 정보:**
{chr(10).join(files_info)}

**분석 중점 사항:**
1. **전체적인 변경사항 이해**: 비즈니스 로직과 기술적 의미
2. **리팩터링 제안**: 코드 구조 개선, 중복 제거, 가독성 향상
3. **로직 오류 위험**: 비즈니스 로직 실수, 엣지 케이스 누락
4. **버그 가능성**: 런타임 오류, 메모리 누수, 동시성 문제
5. **API 설계**: 인터페이스 일관성, 에러 처리
6. **성능 이슈**: 비효율적인 알고리즘, 데이터베이스 쿼리 최적화

**요청사항:**
다음 형식으로 정확히 작성해주세요:

## 📝 Walkthrough

[전체적인 변경사항의 목적과 주요 내용을 2-3문장으로 요약. 비즈니스 가치와 기술적 의미를 포함하여 작성해주세요.]

## Changes

| File Path | Change Summary |
|-----------|---------------|
{self.generate_changes_table_template(changed_files)}

## 🚨 Critical Review Points

**🔴 High Priority (버그 위험)**
- [런타임 오류 가능성이 있는 부분을 구체적으로 명시]
- [메모리 누수나 성능 저하 우려사항]

**🟡 Medium Priority (리팩터링 제안)**
- [코드 구조 개선이 필요한 부분]
- [중복 코드 제거나 추상화 제안]

**🔵 Low Priority (개선 아이디어)**
- [가독성 향상을 위한 제안]
- [미래 확장성을 고려한 개선사항]

## 💡 Recommendations

- [구체적이고 실행 가능한 개선 방안]
- [베스트 프랙티스 적용 제안]
- [추가 테스트 케이스 제안]

**참고:**
- 정적 분석은 SonarQube가 담당하므로 코드 스타일은 제외
- 비즈니스 로직과 아키텍처 관점에서 분석
- 실제 개발자가 놓칠 수 있는 부분에 집중
- 구체적이고 실행 가능한 제안 제시
"""

        try:
            response = self.openai_client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": "당신은 시니어 개발자로서 전문적인 코드 리뷰 전문가입니다. 정적 분석 도구가 놓치는 고차원적인 문제를 찾아내고 건설적인 제안을 제공하세요."},
                    {"role": "user", "content": analysis_prompt}
                ],
                max_tokens=2500,
                temperature=0.2
            )

            return response.choices[0].message.content

        except Exception as e:
            return f"❌ PR Walkthrough 생성 중 오류가 발생했습니다: {str(e)}"

    def generate_changes_table_template(self, changed_files):
        """Changes 테이블 템플릿 생성"""
        template_rows = []
        for file in changed_files:
            # 상태에 따른 이모지 추가
            status_emoji = {
                'added': '➕',
                'modified': '📝',
                'removed': '❌',
                'renamed': '📛'
            }
            emoji = status_emoji.get(file['status'], '📝')
            template_rows.append(f"| {emoji} {file['filename']} | [AI가 이 파일의 주요 변경사항을 분석하여 요약] |")
        return "\n".join(template_rows)

    def generate_tips_section(self):
        """전문적인 Tips 섹션 생성"""
        related_prs = self.find_related_prs()

        tips_section = """

## 🪧 Tips

### 💬 AI 리뷰 명령어
- `/ai-review` - 전체 AI 리뷰 재실행
- 코드 리뷰 코멘트에 질문하여 AI와 대화 가능

### 🔍 Possibly Related PRs"""

        if related_prs:
            for pr in related_prs:
                state_emoji = "✅" if pr['state'] == 'closed' else "🔄"
                tips_section += f"\n- {state_emoji} [#{pr['number']}](https://github.com/{self.repo_name}/pull/{pr['number']}): {pr['title']}"
        else:
            tips_section += "\n- 관련된 최근 PR이 없습니다."

        return tips_section

    def remove_previous_ai_comments(self):
        """이전 AI 분석 코멘트들 삭제 또는 minimize 처리"""
        try:
            comments = self.pr.get_issue_comments()

            for comment in comments:
                # AI 봇이 작성한 코멘트 찾기
                if (comment.user.login == 'github-actions[bot]' and
                    '📝 Walkthrough' in comment.body):
                    try:
                        comment.delete()
                        print(f"✅ 이전 AI 분석 코멘트 삭제: {comment.id}")
                    except Exception as e:
                        print(f"⚠️ 코멘트 삭제 실패, minimize 처리 시도: {e}")
                        # 삭제 실패 시 minimize 처리
                        try:
                            minimized_body = f"<!-- Minimized by new analysis -->\n<details>\n<summary>이전 분석 결과 (클릭하여 보기)</summary>\n\n{comment.body}\n</details>"
                            comment.edit(minimized_body)
                            print(f"✅ 이전 코멘트 minimize 처리: {comment.id}")
                        except Exception as e2:
                            print(f"❌ 코멘트 처리 완전 실패: {e2}")

        except Exception as e:
            print(f"⚠️ 이전 코멘트 정리 중 오류: {e}")

    def post_walkthrough_comment(self, walkthrough_content):
        """Walkthrough 분석 결과를 PR에 코멘트로 등록"""

        # 이전 AI 분석 코멘트 정리
        self.remove_previous_ai_comments()

        # Tips 섹션 추가
        tips_section = self.generate_tips_section()
        final_content = walkthrough_content + tips_section

        # 코멘트 생성
        try:
            comment = self.pr.create_issue_comment(final_content)
            print(f"✅ AI Walkthrough 코멘트 등록 완료: {comment.html_url}")
            return True
        except Exception as e:
            print(f"❌ Walkthrough 코멘트 등록 실패: {e}")
            return False

    def run_analysis(self):
        """전체 분석 프로세스 실행"""
        print("🚀 AI PR 분석을 시작합니다...")

        # 1. 프로젝트 컨텍스트 파악
        print("🔍 프로젝트 기술 스택을 파악하는 중...")
        project_context = self.get_project_context()
        print(f"📊 감지된 기술 스택: {project_context}")

        # 2. 변경된 파일 정보 가져오기
        print("📁 변경된 파일 정보를 수집하는 중...")
        changed_files = self.get_changed_files_content()

        if not changed_files:
            print("❌ 변경된 파일이 없습니다.")
            return

        print(f"📊 총 {len(changed_files)}개 파일이 변경되었습니다.")

        # 3. Walkthrough Summary 생성
        print("🤖 AI Walkthrough 분석을 생성하는 중...")
        walkthrough_content = self.generate_walkthrough_summary(changed_files, project_context)

        # 4. PR에 코멘트로 등록
        print("💬 PR에 Walkthrough 코멘트를 등록하는 중...")
        walkthrough_success = self.post_walkthrough_comment(walkthrough_content)

        # 최종 결과 출력
        if walkthrough_success:
            print("✅ AI PR Walkthrough 분석이 완료되었습니다!")
            print("🎯 전문적인 Walkthrough 코멘트가 생성되었습니다")
        else:
            print("❌ AI PR 분석에 실패했습니다.")

if __name__ == "__main__":
    analyzer = PRAnalyzer()
    analyzer.run_analysis()