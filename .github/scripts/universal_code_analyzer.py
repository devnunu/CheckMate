# .github/scripts/universal_code_analyzer.py
from abc import ABC, abstractmethod
from typing import Dict, List, Optional, Set
import os
import re
from github import Github

class LanguageLinter(ABC):
    """언어별 린터 인터페이스"""

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
    def parse_config(self, config_content: str, config_file: str) -> Dict:
        """설정 파일 파싱"""
        pass

    @abstractmethod
    def get_default_rules(self) -> Dict:
        """기본 린트 규칙"""
        pass

    @abstractmethod
    def check_violations(self, file_content: str, file_path: str, config: Dict) -> List[Dict]:
        """린트 규칙 위반 검사"""
        pass

class KotlinLinter(LanguageLinter):
    """Kotlin ktlint 린터"""

    def get_language_name(self) -> str:
        return "kotlin"

    def get_file_extensions(self) -> List[str]:
        return [".kt", ".kts"]

    def get_config_files(self) -> List[str]:
        return [".editorconfig", "ktlint.conf"]

    def parse_config(self, config_content: str, config_file: str) -> Dict:
        """ktlint 설정 파싱"""
        config = {
            'code_style': 'official',
            'disabled_rules': set(),
            'custom_settings': {}
        }

        if config_file == ".editorconfig":
            return self._parse_editorconfig(config_content)
        elif config_file == "ktlint.conf":
            return self._parse_ktlint_conf(config_content)

        return config

    def _parse_editorconfig(self, content: str) -> Dict:
        """editorconfig 파싱 (기존 로직)"""
        config = {'disabled_rules': set(), 'custom_settings': {}}

        lines = content.split('\n')
        current_section = None

        for line in lines:
            line = line.strip()
            if not line or line.startswith('#'):
                continue

            if line.startswith('[') and line.endswith(']'):
                current_section = line[1:-1]
                continue

            if current_section in ['*.kt', '*'] or current_section is None:
                if '=' in line:
                    key, value = line.split('=', 1)
                    key, value = key.strip(), value.strip()

                    if key.startswith('ktlint_'):
                        if value.lower() == 'disabled':
                            config['disabled_rules'].add(key)
                        else:
                            config['custom_settings'][key] = value

        return config

    def get_default_rules(self) -> Dict:
        """ktlint 기본 규칙"""
        return {
            'indent': {
                'description': '들여쓰기는 4칸 스페이스 사용',
                'pattern': r'^[ ]{0,3}[^ ]|^\t',
                'message': '들여쓰기는 4칸 스페이스를 사용해야 합니다.',
                'priority': 'P3'
            },
            'max-line-length': {
                'description': '최대 라인 길이 120자',
                'pattern': r'^.{121,}$',
                'message': '한 줄의 길이가 120자를 초과합니다.',
                'priority': 'P3'
            },
            'no-wildcard-imports': {
                'description': '와일드카드 import 금지',
                'pattern': r'import\s+.*\.\*',
                'message': '와일드카드 import(*)는 사용하지 마세요.',
                'priority': 'P3'
            },
            'function-naming': {
                'description': '함수명은 camelCase',
                'pattern': r'fun\s+[A-Z][a-zA-Z0-9_]*\s*\(',
                'message': '함수명은 camelCase를 사용해야 합니다.',
                'priority': 'P3'
            }
        }

    def check_violations(self, file_content: str, file_path: str, config: Dict) -> List[Dict]:
        """ktlint 규칙 위반 검사"""
        violations = []
        rules = self.get_default_rules()

        # 비활성화된 규칙 제거
        for disabled_rule in config.get('disabled_rules', []):
            rule_name = disabled_rule.replace('ktlint_standard_', '').replace('ktlint_', '')
            if rule_name in rules:
                del rules[rule_name]

        lines = file_content.split('\n')

        for line_num, line in enumerate(lines, 1):
            for rule_name, rule_config in rules.items():
                if rule_config.get('pattern') and re.search(rule_config['pattern'], line):
                    violations.append({
                        'line': line_num,
                        'rule': rule_name,
                        'priority': rule_config['priority'],
                        'category': 'ktlint',
                        'message': rule_config['message'],
                        'suggestion': self._get_suggestion(rule_name, line)
                    })

        return violations

    def _get_suggestion(self, rule_name: str, line: str) -> str:
        suggestions = {
            'indent': '들여쓰기를 4칸 스페이스로 수정하세요.',
            'max-line-length': '긴 줄을 여러 줄로 나누어 가독성을 높이세요.',
            'no-wildcard-imports': 'import com.example.* → import com.example.SpecificClass',
            'function-naming': 'MyFunction() → myFunction()'
        }
        return suggestions.get(rule_name, '해당 규칙에 맞게 수정해주세요.')

class SwiftLinter(LanguageLinter):
    """Swift SwiftLint 린터"""

    def get_language_name(self) -> str:
        return "swift"

    def get_file_extensions(self) -> List[str]:
        return [".swift"]

    def get_config_files(self) -> List[str]:
        return [".swiftlint.yml", "swiftlint.yml", ".swiftlint.yaml"]

    def parse_config(self, config_content: str, config_file: str) -> Dict:
        """SwiftLint YAML 설정 파싱"""
        import yaml
        try:
            config = yaml.safe_load(config_content) or {}
            return {
                'disabled_rules': set(config.get('disabled_rules', [])),
                'opt_in_rules': set(config.get('opt_in_rules', [])),
                'custom_settings': config
            }
        except:
            return {'disabled_rules': set(), 'opt_in_rules': set(), 'custom_settings': {}}

    def get_default_rules(self) -> Dict:
        """SwiftLint 기본 규칙"""
        return {
            'line_length': {
                'description': '최대 라인 길이 120자',
                'pattern': r'^.{121,}$',
                'message': '한 줄의 길이가 120자를 초과합니다.',
                'priority': 'P3'
            },
            'function_parameter_count': {
                'description': '함수 매개변수 5개 이하',
                'pattern': r'func\s+\w+\([^)]*,[^)]*,[^)]*,[^)]*,[^)]*,[^)]*\)',
                'message': '함수 매개변수가 너무 많습니다. 구조체나 튜플 사용을 고려하세요.',
                'priority': 'P2'
            },
            'force_cast': {
                'description': 'force cast 사용 금지',
                'pattern': r'\s+as!\s+',
                'message': 'force cast(as!) 대신 안전한 캐스팅(as?)을 사용하세요.',
                'priority': 'P2'
            },
            'implicitly_unwrapped_optional': {
                'description': '암시적 옵셔널 언래핑 주의',
                'pattern': r':\s*\w+!',
                'message': '암시적 옵셔널 언래핑(!)보다 명시적 옵셔널을 권장합니다.',
                'priority': 'P2'
            }
        }

    def check_violations(self, file_content: str, file_path: str, config: Dict) -> List[Dict]:
        """SwiftLint 규칙 위반 검사"""
        violations = []
        rules = self.get_default_rules()

        # 비활성화된 규칙 제거
        for disabled_rule in config.get('disabled_rules', []):
            if disabled_rule in rules:
                del rules[disabled_rule]

        lines = file_content.split('\n')

        for line_num, line in enumerate(lines, 1):
            for rule_name, rule_config in rules.items():
                if rule_config.get('pattern') and re.search(rule_config['pattern'], line):
                    violations.append({
                        'line': line_num,
                        'rule': rule_name,
                        'priority': rule_config['priority'],
                        'category': 'swiftlint',
                        'message': rule_config['message'],
                        'suggestion': self._get_suggestion(rule_name, line)
                    })

        return violations

    def _get_suggestion(self, rule_name: str, line: str) -> str:
        suggestions = {
            'line_length': '긴 줄을 여러 줄로 나누어 가독성을 높이세요.',
            'function_parameter_count': '매개변수를 구조체로 그룹화하거나 함수를 분할하세요.',
            'force_cast': 'as! → as? 또는 guard let 사용',
            'implicitly_unwrapped_optional': 'String! → String? 사용 권장'
        }
        return suggestions.get(rule_name, '해당 규칙에 맞게 수정해주세요.')

class JavaScriptLinter(LanguageLinter):
    """JavaScript ESLint 린터"""

    def get_language_name(self) -> str:
        return "javascript"

    def get_file_extensions(self) -> List[str]:
        return [".js", ".jsx", ".ts", ".tsx"]

    def get_config_files(self) -> List[str]:
        return [".eslintrc.json", ".eslintrc.js", "eslint.config.js", "package.json"]

    def parse_config(self, config_content: str, config_file: str) -> Dict:
        """ESLint 설정 파싱"""
        import json
        try:
            if config_file == "package.json":
                package_json = json.loads(config_content)
                eslint_config = package_json.get('eslintConfig', {})
            else:
                eslint_config = json.loads(config_content)

            return {
                'disabled_rules': set(),  # ESLint는 rules에서 "off"로 처리
                'rules': eslint_config.get('rules', {}),
                'extends': eslint_config.get('extends', [])
            }
        except:
            return {'disabled_rules': set(), 'rules': {}, 'extends': []}

    def get_default_rules(self) -> Dict:
        """ESLint 기본 규칙"""
        return {
            'no-unused-vars': {
                'description': '사용하지 않는 변수 제거',
                'pattern': r'(const|let|var)\s+(\w+)(?![^;]*\2)',
                'message': '사용하지 않는 변수를 제거해주세요.',
                'priority': 'P3'
            },
            'prefer-const': {
                'description': 'const 사용 권장',
                'pattern': r'let\s+\w+\s*=\s*[^;]+;(?![^}]*\1\s*=)',
                'message': '재할당하지 않는 변수는 const를 사용하세요.',
                'priority': 'P3'
            },
            'no-console': {
                'description': 'console.log 제거',
                'pattern': r'console\.(log|warn|error)',
                'message': '프로덕션 코드에서 console 사용을 피하세요.',
                'priority': 'P3'
            },
            'eqeqeq': {
                'description': '엄격한 비교 연산자 사용',
                'pattern': r'[^=!]==[^=]|[^=!]!=[^=]',
                'message': '== 대신 ===, != 대신 !==를 사용하세요.',
                'priority': 'P2'
            }
        }

    def check_violations(self, file_content: str, file_path: str, config: Dict) -> List[Dict]:
        """ESLint 규칙 위반 검사"""
        violations = []
        rules = self.get_default_rules()

        # ESLint rules에서 "off"된 규칙 제거
        eslint_rules = config.get('rules', {})
        for rule_name, rule_value in eslint_rules.items():
            if rule_value == "off" or rule_value == 0:
                if rule_name in rules:
                    del rules[rule_name]

        lines = file_content.split('\n')

        for line_num, line in enumerate(lines, 1):
            for rule_name, rule_config in rules.items():
                if rule_config.get('pattern') and re.search(rule_config['pattern'], line):
                    violations.append({
                        'line': line_num,
                        'rule': rule_name,
                        'priority': rule_config['priority'],
                        'category': 'eslint',
                        'message': rule_config['message'],
                        'suggestion': self._get_suggestion(rule_name, line)
                    })

        return violations

    def _get_suggestion(self, rule_name: str, line: str) -> str:
        suggestions = {
            'no-unused-vars': '사용하지 않는 변수를 제거하거나 언더스코어(_)로 시작하세요.',
            'prefer-const': 'let → const 변경',
            'no-console': 'console.log → 로깅 라이브러리 사용',
            'eqeqeq': '== → ===, != → !== 변경'
        }
        return suggestions.get(rule_name, '해당 규칙에 맞게 수정해주세요.')

class UniversalCodeAnalyzer:
    """범용 코드 분석기"""

    def __init__(self, repo, pr):
        self.repo = repo
        self.pr = pr
        self.linters = {
            'kotlin': KotlinLinter(),
            'swift': SwiftLinter(),
            'javascript': JavaScriptLinter()
        }

    def detect_language(self, file_path: str) -> Optional[str]:
        """파일 확장자로 언어 감지"""
        for lang_name, linter in self.linters.items():
            for ext in linter.get_file_extensions():
                if file_path.endswith(ext):
                    return lang_name
        return None

    def get_linter_config(self, language: str) -> Dict:
        """언어별 린터 설정 가져오기"""
        if language not in self.linters:
            return {}

        linter = self.linters[language]

        # 설정 파일 우선순위대로 시도
        for config_file in linter.get_config_files():
            try:
                content = self.repo.get_contents(config_file)
                config_content = content.decoded_content.decode('utf-8')
                return linter.parse_config(config_content, config_file)
            except:
                continue

        return {}  # 설정 파일이 없으면 빈 설정

    def analyze_file(self, file_path: str, file_content: str) -> List[Dict]:
        """파일 분석"""
        language = self.detect_language(file_path)
        if not language:
            return []

        linter = self.linters[language]
        config = self.get_linter_config(language)

        return linter.check_violations(file_content, file_path, config)

    def get_supported_extensions(self) -> Set[str]:
        """지원하는 모든 파일 확장자"""
        extensions = set()
        for linter in self.linters.values():
            extensions.update(linter.get_file_extensions())
        return extensions

    def get_analysis_summary(self) -> str:
        """분석 설정 요약"""
        summary = "🔧 **범용 코드 분석기 설정**\n\n"

        for lang_name, linter in self.linters.items():
            config = self.get_linter_config(lang_name)
            summary += f"### {linter.get_language_name().title()}\n"
            summary += f"- 파일 확장자: {', '.join(linter.get_file_extensions())}\n"
            summary += f"- 설정 파일: {', '.join(linter.get_config_files())}\n"

            if config:
                disabled_count = len(config.get('disabled_rules', []))
                summary += f"- 비활성 규칙: {disabled_count}개\n"
            else:
                summary += f"- 설정: 기본 규칙 사용\n"

            summary += "\n"

        return summary