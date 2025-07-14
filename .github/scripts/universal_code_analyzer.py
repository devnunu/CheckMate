# .github/scripts/universal_code_analyzer.py
from abc import ABC, abstractmethod
from typing import Dict, List, Optional, Set
import os
import re
from github import Github
import openai

class LanguageLinter(ABC):
    """언어별 린터 인터페이스 (AI 기반)"""

    def __init__(self, openai_client):
        self.openai_client = openai_client

    @abstractmethod
    def get_language_name(self) -> str:
        """언어 이름 반환"""
        pass

    @abstractmethod
    def get_file_extensions(self) -> List[str]:
        """지원하는 파일 확장자 목록"""
        pass

    @abstractmethod
    def get_config_files(self) -> List[str]:
        """설정 파일 이름 목록 (우선순위 순)"""
        pass

    @abstractmethod
    def get_linter_description(self) -> str:
        """린터 도구 설명 (AI가 이해할 수 있는 형태)"""
        pass

    def analyze_with_ai(self, file_content: str, file_path: str, config_content: str) -> List[Dict]:
        """AI를 사용한 린트 분석"""

        analysis_prompt = f"""
당신은 {self.get_language_name()} 전문 린터입니다. 다음 파일을 분석하여 린트 규칙 위반을 찾아주세요.

**파일:** {file_path}
**언어:** {self.get_language_name()}

**린터 도구 정보:**
{self.get_linter_description()}

**프로젝트 설정 파일:**
```
{config_content}
```

**분석할 코드:**
```{self.get_language_name()}
{file_content[:3000]}  # 토큰 제한으로 일부만
```

**분석 요청:**
위 설정 파일의 규칙에 따라 코드를 검사하고, 위반사항을 찾아 JSON 배열로 응답해주세요.

각 위반사항은 다음 형식으로:
```json
[
  {{
    "line": 줄번호,
    "rule": "규칙명 (예: indent, max-line-length, function-naming)",
    "priority": "P3",
    "category": "{self.get_language_name().lower()}lint",
    "message": "구체적인 문제 설명",
    "suggestion": "수정된 코드 예시"
  }}
]
```

**중요:**
- 설정 파일에서 disabled된 규칙은 검사하지 마세요
- 실제 위반이 있는 줄 번호만 정확히 지정하세요
- 문제가 없으면 빈 배열 [] 반환
- JSON 형식만 응답하고 다른 텍스트는 포함하지 마세요
"""

        try:
            response = self.openai_client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": f"{self.get_language_name()} 린터 전문가로서 설정 파일 기반으로 정확한 코드 검사를 수행합니다."},
                    {"role": "user", "content": analysis_prompt}
                ],
                max_tokens=1500,
                temperature=0.1
            )

            response_text = response.choices[0].message.content.strip()

            import json
            try:
                violations = json.loads(response_text)
                return violations if isinstance(violations, list) else []
            except json.JSONDecodeError:
                print(f"AI 린트 분석 JSON 파싱 실패: {response_text[:200]}")
                return []

        except Exception as e:
            print(f"AI 린트 분석 실패: {e}")
            return []

class KotlinLinter(LanguageLinter):
    """Kotlin ktlint 린터 (AI 기반)"""

    def get_language_name(self) -> str:
        return "kotlin"

    def get_file_extensions(self) -> List[str]:
        return [".kt", ".kts"]

    def get_config_files(self) -> List[str]:
        return [".editorconfig", "ktlint.conf"]

    def get_linter_description(self) -> str:
        return """
ktlint는 Kotlin 코드 스타일 린터입니다.

주요 규칙:
- ktlint_standard_indent: 들여쓰기 (기본 4칸 스페이스)
- ktlint_standard_max-line-length: 최대 라인 길이 (기본 120자)
- ktlint_standard_no-wildcard-imports: 와일드카드 import 금지
- ktlint_standard_function-naming: 함수명 camelCase
- ktlint_standard_property-naming: 프로퍼티명 camelCase
- ktlint_standard_enum-entry-name-case: enum 항목 UPPER_SNAKE_CASE
- ktlint_code_style: android_studio 또는 official

설정에서 "disabled"로 표시된 규칙은 검사하지 않습니다.
"""

class SwiftLinter(LanguageLinter):
    """Swift SwiftLint 린터 (AI 기반)"""

    def get_language_name(self) -> str:
        return "swift"

    def get_file_extensions(self) -> List[str]:
        return [".swift"]

    def get_config_files(self) -> List[str]:
        return [".swiftlint.yml", "swiftlint.yml", ".swiftlint.yaml"]

    def get_linter_description(self) -> str:
        return """
SwiftLint는 Swift 코드 스타일 린터입니다.

주요 규칙:
- line_length: 최대 라인 길이 (기본 120자)
- function_parameter_count: 함수 매개변수 개수 제한
- force_cast: force cast (as!) 사용 금지
- implicitly_unwrapped_optional: 암시적 옵셔널 언래핑 주의
- identifier_name: 변수/함수명 규칙
- type_name: 타입명 규칙

disabled_rules에 포함된 규칙은 검사하지 않습니다.
opt_in_rules에 포함된 규칙만 추가로 검사합니다.
"""

class JavaScriptLinter(LanguageLinter):
    """JavaScript ESLint 린터 (AI 기반)"""

    def get_language_name(self) -> str:
        return "javascript"

    def get_file_extensions(self) -> List[str]:
        return [".js", ".jsx", ".ts", ".tsx"]

    def get_config_files(self) -> List[str]:
        return [".eslintrc.json", ".eslintrc.js", "eslint.config.js", "package.json"]

    def get_linter_description(self) -> str:
        return """
ESLint는 JavaScript/TypeScript 코드 린터입니다.

주요 규칙:
- no-unused-vars: 사용하지 않는 변수
- prefer-const: const 사용 권장
- no-console: console.log 사용 금지
- eqeqeq: 엄격한 비교 연산자 (===, !==)
- indent: 들여쓰기 규칙
- quotes: 따옴표 스타일
- semi: 세미콜론 사용

rules에서 "off" 또는 0으로 설정된 규칙은 검사하지 않습니다.
extends 설정도 고려해주세요.
"""

class UniversalCodeAnalyzer:
    """AI 기반 범용 코드 분석기"""

    def __init__(self, repo, pr):
        self.repo = repo
        self.pr = pr
        self.openai_client = openai.OpenAI(api_key=os.environ['OPENAI_API_KEY'])

        # AI 기반 린터들 초기화
        self.linters = {
            'kotlin': KotlinLinter(self.openai_client),
            'swift': SwiftLinter(self.openai_client),
            'javascript': JavaScriptLinter(self.openai_client)
        }

    def detect_language(self, file_path: str) -> Optional[str]:
        """파일 확장자로 언어 감지"""
        for lang_name, linter in self.linters.items():
            for ext in linter.get_file_extensions():
                if file_path.endswith(ext):
                    return lang_name
        return None

    def get_linter_config_content(self, language: str) -> str:
        """언어별 린터 설정 파일 내용 가져오기"""
        if language not in self.linters:
            return ""

        linter = self.linters[language]

        # 설정 파일 우선순위대로 시도
        for config_file in linter.get_config_files():
            try:
                content = self.repo.get_contents(config_file)
                config_content = content.decoded_content.decode('utf-8')
                print(f"✅ {language} 설정 파일 발견: {config_file}")
                return config_content
            except:
                continue

        print(f"⚠️ {language} 설정 파일 없음 - 기본 규칙 사용")
        return "# 설정 파일이 없습니다. 기본 규칙을 사용합니다."

    def analyze_file(self, file_path: str, file_content: str) -> List[Dict]:
        """AI 기반 파일 분석"""
        language = self.detect_language(file_path)
        if not language:
            return []

        linter = self.linters[language]
        config_content = self.get_linter_config_content(language)

        return linter.analyze_with_ai(file_content, file_path, config_content)

    def get_supported_extensions(self) -> Set[str]:
        """지원하는 모든 파일 확장자"""
        extensions = set()
        for linter in self.linters.values():
            extensions.update(linter.get_file_extensions())
        return extensions

    def get_analysis_summary(self) -> str:
        """분석 설정 요약"""
        summary = "🤖 **AI 기반 범용 코드 분석기**\n\n"

        for lang_name, linter in self.linters.items():
            config_content = self.get_linter_config_content(lang_name)
            has_config = len(config_content) > 50  # 실제 설정이 있는지 확인

            summary += f"### {linter.get_language_name().title()}\n"
            summary += f"- 파일 확장자: {', '.join(linter.get_file_extensions())}\n"
            summary += f"- 린터: {linter.get_linter_description().split('.')[0]}\n"
            summary += f"- 설정: {'프로젝트 설정 사용' if has_config else '기본 규칙 사용'}\n\n"

        return summary