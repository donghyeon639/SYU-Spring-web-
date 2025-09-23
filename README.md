## git clone으로 프로젝트 얻는 방법 💻
1. 저장소 주소 복사하기

먼저, 파일을 얻고 싶은 Git 저장소 페이지(예: GitHub)로 이동하세요.

초록색 < > Code 버튼을 클릭합니다.

HTTPS 탭에 있는 URL을 복사합니다. 주소 옆에 있는 복사 아이콘을 누르면 편리합니다.

2. 터미널(Terminal) 열기

Windows: 명령 프롬프트(CMD), PowerShell, 또는 Git Bash를 실행합니다.

macOS: 터미널(Terminal)을 실행합니다.

3. git clone 명령어 실행하기

터미널에서 아래 명령어를 입력하고 Enter 키를 누릅니다. <붙여넣을 주소> 부분에 1번에서 복사한 주소를 붙여넣으세요.

"
git clone https://github.com/donghyeon639/SYU-Spring-web-.git
"

## Clone 후 새 브랜치에서 변경사항 올리는 법 🌿
전체 흐름은 ① 브랜치 생성 → ② 작업 및 저장 → ③ 원격 저장소에 올리기 순서로 진행됩니다.

1. 새 브랜치 생성 및 이동하기

먼저, 작업을 위한 자신만의 공간인 새 브랜치(branch)를 만듭니다. checkout -b 옵션을 사용하면 브랜치를 만들고 바로 그 브랜치로 이동할 수 있습니다.

Bash

# [브랜치-이름] 부분에 원하는 이름을 넣으세요 (예: feature/login, fix/readme-typo)
git checkout -b [브랜치-이름]
2. 파일 수정 및 변경사항 저장하기 (Add & Commit)

이제 원하는 대로 파일을 수정, 추가, 삭제하는 작업을 자유롭게 하세요. 작업이 일단락되면, 변경된 내용을 저장해야 합니다. Git에서는 add와 commit 두 단계로 저장합니다.

Bash

# 1. 변경된 모든 파일을 스테이징(Staging) 영역으로 이동
git add .

# 2. 스테이징된 파일들을 "어떤 작업을 했는지" 메시지와 함께 로컬 저장소에 확정(Commit)
git commit -m "여기에 변경 내용 요약 작성 (예: README 파일 오타 수정)"
git add .: 현재 폴더(.은 현재 위치를 의미)에서 변경된 모든 파일을 "곧 저장할 것"이라는 대기실(Staging Area)로 옮기는 과정입니다.

git commit: 대기실에 있는 파일들을 하나의 작업 단위로 묶어 메시지와 함께 로컬 저장소에 영구적으로 기록하는 과정입니다.

3. 원격 저장소에 올리기 (Push)

로컬 저장소에 기록한 변경사항을 이제 다른 사람들과 공유할 수 있도록 원격 저장소(GitHub 등)에 업로드합니다.

Bash

# [브랜치-이름]에 1번에서 만들었던 브랜치 이름을 정확히 입력합니다.
git push origin [브랜치-이름]
이 명령어를 실행하면, origin(일반적으로 GitHub의 내 원격 저장소를 가리킴)에 [브랜치-이름]이라는 브랜치가 새로 만들어지고 로컬에서 커밋했던 내용이 모두 업로드됩니다.

## 요약: 필수 명령어 3단계
Bash

# 1. 'feature/new-function' 이라는 브랜치를 만들고 이동
"
git checkout -b feature/new-function
"
# 2. 작업 후 변경사항 저장
"
git add .
git commit -m "새로운 기능 추가"
"

# 3. 원격 저장소에 푸시
"
git push origin feature/new-function
"

