# .github/scripts/interactive_ai_responder.py
import os
import json
import openai
from github import Github
import re
from datetime import datetime

class InteractiveAIResponder:
    def __init__(self):
        self.openai_client = openai.OpenAI(api_key=os.environ['OPENAI_API_KEY'])
        self.github_client = Github(os.environ['GITHUB_TOKEN'])
        self.repo_name = os.environ['REPO_NAME']
        self.comment_id = int(os.environ['COMMENT_ID'])
        self.comment_body = os.environ.get('COMMENT_BODY', '')
        self.comment_author = os.environ.get('COMMENT_AUTHOR', '')

        self.repo = self.github_client.get_repo(self.repo_name)

    def is_ai_generated_comment(self, comment) -> bool:
        """AI가 생성한 코멘트인지 확인"""
        if comment.user.login != 'github-actions[bot]':
            return False

        # AI 생성 코멘트의 특징적 패턴들
        ai_patterns = [
            '⚠️ **Potential issue**',
            '🔧 **Refactor suggestion**',
            '📝 **Code quality**',
            '📝 Walkthrough',
            '🤖 **AI',
            'Committable suggestion'
        ]

        return any(pattern in comment.body for pattern in ai_patterns)

    def find_parent_ai_comment(self):
        """현재 코멘트의 부모 AI 코멘트 찾기"""
        try:
            # 현재 코멘트 가져오기
            current_comment = None

            # PR 코멘트인지 확인
            try:
                current_comment = self.github_client.get_repo(self.repo_name).get_issue_comment(self.comment_id)
                pr_number = current_comment.issue_url.split('/')[-1]
                pr = self.repo.get_pull(int(pr_number))

                # Issue 코멘트 방식
                comments = pr.get_issue_comments()

            except:
                # Review 코멘트 방식 시도
                pr_number = self.find_pr_from_review_comment()
                if not pr_number:
                    return None, None

                pr = self.repo.get_pull(pr_number)
                comments = pr.get_review_comments()

            # 현재 코멘트 이전의 AI 코멘트들 찾기
            ai_comments = []
            for comment in comments:
                if comment.id == self.comment_id:
                    break
                if self.is_ai_generated_comment(comment):
                    ai_comments.append(comment)

            if not ai_comments:
                return None, None

            # 가장 최근의 AI 코멘트 반환
            parent_comment = ai_comments[-1]
            return parent_comment, pr

        except Exception as e:
            print(f"부모 AI 코멘트 찾기 실패: {e}")
            return None, None

    def find_pr_from_review_comment(self):
        """Review 코멘트에서 PR 번호 찾기"""
        try:
            # GitHub API로 모든 열린 PR 확인
            pulls = self.repo.get_pulls(state='open')
            for pr in pulls:
                review_comments = pr.get_review_comments()
                for comment in review_comments:
                    if comment.id == self.comment_id:
                        return pr.number
            return None
        except:
            return None

    def get_code_context(self, pr, parent_comment):
        """코드 컨텍스트 가져오기"""
        try:
            context = {
                'file_path': None,
                'line_number': None,
                'surrounding_code': '',
                'diff_context': ''
            }

            # Review 코멘트인 경우
            if hasattr(parent_comment, 'path'):
                context['file_path'] = parent_comment.path
                context['line_number'] = getattr(parent_comment, 'line', None)

                # 파일 내용 가져오기
                try:
                    file_content = self.repo.get_contents(parent_comment.path, ref=pr.head.sha)
                    lines = file_content.decoded_content.decode('utf-8').split('\n')

                    if context['line_number']:
                        start = max(0, context['line_number'] - 5)
                        end = min(len(lines), context['line_number'] + 5)
                        context['surrounding_code'] = '\n'.join(f"{i+1:3d}: {lines[i]}" for i in range(start, end))

                except Exception as e:
                    print(f"파일 내용 가져오기 실패: {e}")

            # Diff 정보 가져오기
            try:
                files = pr.get_files()
                for file in files:
                    if context['file_path'] and file.filename == context['file_path']:
                        context['diff_context'] = file.patch[:1000] if file.patch else ''
                        break
            except Exception as e:
                print(f"Diff 정보 가져오기 실패: {e}")

            return context

        except Exception as e:
            print(f"코드 컨텍스트 가져오기 실패: {e}")
            return {'file_path': None, 'line_number': None, 'surrounding_code': '', 'diff_context': ''}

    def extract_conversation_context(self, parent_comment, user_comment):
        """대화 컨텍스트 추출"""
        try:
            # AI 코멘트에서 주요 정보 추출
            ai_context = {
                'issue_type': 'general',
                'issue_title': '',
                'issue_description': '',
                'suggested_code': ''
            }

            # 이슈 타입 추출
            if '⚠️ **Potential issue**' in parent_comment.body:
                ai_context['issue_type'] = 'potential_issue'
            elif '🔧 **Refactor suggestion**' in parent_comment.body:
                ai_context['issue_type'] = 'refactor_suggestion'
            elif '📝 **Code quality**' in parent_comment.body:
                ai_context['issue_type'] = 'code_quality'

            # 제목 추출
            title_match = re.search(r'\*\*([^*]+)\*\*', parent_comment.body)
            if title_match:
                ai_context['issue_title'] = title_match.group(1)

            # 코드 제안 추출
            code_blocks = re.findall(r'```[\w]*\n(.*?)\n```', parent_comment.body, re.DOTALL)
            if code_blocks:
                ai_context['suggested_code'] = code_blocks[0]

            return ai_context

        except Exception as e:
            print(f"대화 컨텍스트 추출 실패: {e}")
            return {'issue_type': 'general', 'issue_title': '', 'issue_description': '', 'suggested_code': ''}

    def generate_ai_response(self, parent_comment, user_comment, code_context, conversation_context):
        """AI 응답 생성"""

        response_prompt = f"""
당신은 코드 리뷰를 도와주는 AI 어시스턴트입니다. 개발자가 당신의 이전 코멘트에 질문이나 의견을 남겼습니다.

**이전 AI 코멘트:**
{parent_comment.body[:1000]}

**개발자 질문/의견:**
{user_comment}

**코드 컨텍스트:**
파일: {code_context.get('file_path', 'N/A')}
라인: {code_context.get('line_number', 'N/A')}

주변 코드:
```
{code_context.get('surrounding_code', 'N/A')}
```

Diff:
```diff
{code_context.get('diff_context', 'N/A')[:500]}
```

**대화 컨텍스트:**
- 이슈 타입: {conversation_context.get('issue_type')}
- 이슈 제목: {conversation_context.get('issue_title')}

**응답 가이드라인:**
1. 개발자의 질문에 구체적이고 도움이 되는 답변 제공
2. 코드 컨텍스트를 바탕으로 한 실용적인 조언
3. 필요시 대안 솔루션 제시
4. 친근하고 전문적인 톤 유지
5. 한국어로 자연스럽게 응답

**응답 형식:**
@{self.comment_author} [자연스러운 인사말]. [구체적인 답변 내용]. [필요시 추가 제안이나 코드 예시]

답변은 간결하면서도 충분한 정보를 포함해야 합니다.
"""

        try:
            response = self.openai_client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": "당신은 친근하고 전문적인 코드 리뷰 AI 어시스턴트입니다. 개발자의 질문에 도움이 되는 구체적인 답변을 제공하세요."},
                    {"role": "user", "content": response_prompt}
                ],
                max_tokens=1000,
                temperature=0.3
            )

            return response.choices[0].message.content.strip()

        except Exception as e:
            print(f"AI 응답 생성 실패: {e}")
            return f"@{self.comment_author} 죄송합니다. 현재 응답을 생성할 수 없습니다. 나중에 다시 시도해주세요."

    def post_ai_response(self, parent_comment, ai_response):
        """AI 응답을 코멘트로 게시"""
        try:
            # Review 코멘트에 대한 응답인 경우
            if hasattr(parent_comment, 'path'):
                # Review 코멘트에는 직접 응답할 수 없으므로 PR에 일반 코멘트로 게시
                pr_url = parent_comment.pull_request_url
                pr_number = int(pr_url.split('/')[-1])
                pr = self.repo.get_pull(pr_number)

                response_body = f"**💬 AI 응답**\n\n{ai_response}\n\n*[라인별 코멘트](#{parent_comment.id})에 대한 응답*"
                comment = pr.create_issue_comment(response_body)

            else:
                # Issue 코멘트에 대한 응답
                response_body = f"**💬 AI 응답**\n\n{ai_response}"

                # 부모 코멘트가 속한 PR 찾기
                pr_number = parent_comment.issue_url.split('/')[-1]
                pr = self.repo.get_pull(int(pr_number))
                comment = pr.create_issue_comment(response_body)

            print(f"✅ AI 응답이 게시되었습니다: {comment.html_url}")
            return True

        except Exception as e:
            print(f"❌ AI 응답 게시 실패: {e}")
            return False

    def should_respond_to_comment(self, user_comment):
        """응답할 가치가 있는 코멘트인지 판단"""

        # 응답하지 않을 패턴들
        ignore_patterns = [
            r'^(감사|고마워|thanks|thx)[\s\S]*$',  # 단순 감사 인사
            r'^(ok|okay|좋아|알겠)[\s\S]*$',      # 단순 동의
            r'^[\s\S]*\+1[\s\S]*$',               # +1 형태
            r'^[\s\S]*👍[\s\S]*$',                # 이모지만
            r'^[\s\S]*✅[\s\S]*$',                # 체크 이모지
        ]

        comment_lower = user_comment.lower().strip()

        # 너무 짧은 코멘트 (5자 이하)
        if len(comment_lower) <= 5:
            return False

        # 무시할 패턴에 매치되는지 확인
        for pattern in ignore_patterns:
            if re.match(pattern, comment_lower, re.IGNORECASE):
                return False

        # 질문 형태나 구체적인 의견이 있는지 확인
        question_indicators = ['?', '어떻게', '왜', '어떤', '언제', '어디서', '뭔가', '그런데', '근데', '그럼', '만약']

        if any(indicator in user_comment for indicator in question_indicators):
            return True

        # 15자 이상의 구체적인 코멘트
        if len(user_comment.strip()) >= 15:
            return True

        return False

    def run_interactive_response(self):
        """대화형 응답 전체 프로세스 실행"""
        print("🤖 대화형 AI 응답을 시작합니다...")

        # 1. 응답할 가치가 있는 코멘트인지 확인
        if not self.should_respond_to_comment(self.comment_body):
            print(f"⏭️ 단순한 코멘트로 판단, 응답 건너뛰기: '{self.comment_body[:50]}...'")
            return

        print(f"📝 사용자 코멘트: {self.comment_author} - '{self.comment_body[:100]}...'")

        # 2. 부모 AI 코멘트 찾기
        print("🔍 부모 AI 코멘트를 찾는 중...")
        parent_comment, pr = self.find_parent_ai_comment()

        if not parent_comment:
            print("⚠️ 부모 AI 코멘트를 찾을 수 없습니다.")
            return

        print(f"✅ 부모 AI 코멘트 발견: ID {parent_comment.id}")

        # 3. 코드 컨텍스트 수집
        print("📄 코드 컨텍스트를 수집하는 중...")
        code_context = self.get_code_context(pr, parent_comment)

        # 4. 대화 컨텍스트 추출
        print("💬 대화 컨텍스트를 분석하는 중...")
        conversation_context = self.extract_conversation_context(parent_comment, self.comment_body)

        # 5. AI 응답 생성
        print("🤖 AI 응답을 생성하는 중...")
        ai_response = self.generate_ai_response(
            parent_comment,
            self.comment_body,
            code_context,
            conversation_context
        )

        # 6. 응답 게시
        print("💬 응답을 게시하는 중...")
        success = self.post_ai_response(parent_comment, ai_response)

        if success:
            print("✅ 대화형 AI 응답이 완료되었습니다!")
        else:
            print("❌ 대화형 AI 응답에 실패했습니다.")

if __name__ == "__main__":
    responder = InteractiveAIResponder()
    responder.run_interactive_response()
