# Elytra Boundary — Fabric 1.21.1

BetterMC 3 Fabric 서버에서 지정된 원형 범위 밖으로 겉날개 비행하는 플레이어를 킥하는 서버 전용 모드입니다.

## 기본 설정

- 차원: 오버월드만
- 중심 X/Z: `1141`, `2548`
- 반경: `3000`블록
- 판정: Y를 무시한 XZ 원형 거리
- 검사 주기: 5틱
- 범위 밖에서 실제 겉날개 활공 중이면 킥
- 클라이언트 설치 불필요

첫 실행 후 다음 파일이 생성됩니다.

```text
config/elytra-boundary.json
```

좌표, 반경, 검사 주기, 킥 메시지, 예외 플레이어를 이 파일에서 변경한 뒤 서버를 재시작하세요.

## GitHub Actions로 빌드

1. GitHub에서 새 빈 저장소를 만듭니다.
2. 이 프로젝트의 **내용물 전체**를 저장소 최상단에 업로드합니다.  
   `.github`, `src`, `build.gradle`이 같은 최상단에 있어야 합니다.
3. `Commit changes`를 누릅니다.
4. 저장소의 **Actions** 탭에서 `Build Fabric Mod`를 엽니다.
5. 완료된 실행 아래의 **Artifacts**에서 `elytra-boundary-fabric-1.21.1`을 내려받습니다.
6. ZIP 안의 `elytra-boundary-1.0.0.jar`를 PebbleHost 서버의 `mods` 폴더에 넣습니다.
7. 서버를 완전히 재시작합니다.

Actions가 자동 실행되지 않으면:

1. `Actions` → `Build Fabric Mod`
2. `Run workflow`
3. `Run workflow` 버튼

## Release로 JAR 받기

`v1.0.0` 같은 태그를 푸시하면 Actions가 GitHub Release를 만들거나 기존 Release에 JAR를 첨부합니다.

```bash
git tag v1.0.0
git push origin v1.0.0
```

일반적인 사용에는 Release가 필요 없으며 Actions의 Artifact 다운로드만으로 충분합니다.

## 서버 설정 예시

```json
{
  "enabled": true,
  "centerX": 1141.0,
  "centerZ": 2548.0,
  "radius": 3000.0,
  "checkIntervalTicks": 5,
  "kickMessage": "프리젠 영역 밖에서는 겉날개를 사용할 수 없습니다.",
  "overworldOnly": true,
  "exemptPlayers": [
    "YourMinecraftName"
  ]
}
```

## 주의

- 범위 밖에 있는 것만으로는 킥되지 않습니다. 범위 밖에서 `FallFlying` 상태일 때만 킥됩니다.
- 모드는 월드보더를 만들지 않으며 걷기, 보트, 말 등의 이동은 제한하지 않습니다.
- 설정 변경 후 `/reload`가 아니라 서버 재시작이 필요합니다.
