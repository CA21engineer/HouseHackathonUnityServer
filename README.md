# HouseHackathonUnityServer

## 動作確認
※モックを組み込んでいるので本番とはレスポンスが違う

```bash
$ export SERVER_ENDPOINT=http://localhost:18080
or
$ export SERVER_ENDPOINT=http://35.243.119.173:18080
```

1. build, run
```bash
$ chmod +x run-local.sh
$ ./run-local.sh
...
// 初回はめっちゃ時間かかる...
```

2. install grpcurl
```bash
$ brew install grpcurl
```

### CreateRoom
```bash
// rpc CreateRoom(CreateRoomRequest) returns (stream RoomResponse) {};
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"AccountId":"bambootuna","roomKey":""}' ${SERVER_ENDPOINT} room.RoomService/CreateRoom

Response contents:
{
  "createRoomResponse": {
    "RoomId": "mock_room_id"
  }
}

Response contents:
{
  "joinRoomResponse": {
    "RoomId": "mock_room_id"
  }
}

Response contents:
{
  "readyResponse": {
    "Member": [
      {
        "AccountId": "bambootuna"
      }
    ],
    "date": "2020-05-03T03:57:57.812Z"
  }
}

```

### ConnectPlayingData
```bash
// rpc ConnectPlayingData(stream PlayingData) returns (stream PlayingData) {};
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"RoomId":"roomId","Coordinate":{"x":0,"y":0,"date":"2020-05-03T03:57:57.812Z"}}' ${SERVER_ENDPOINT} room.RoomService/ConnectPlayingData

Response contents:
{
  "RoomId": "roomId",
  "Coordinate": {
    "date": "2020-05-03T03:57:57.812Z"
  }
}

```

### ChildOperation
```bash
// rpc ChildOperation(stream Operation) returns (Empty) {};
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"RoomId":"roomId","Direction":0,"strength":0.12345}' ${SERVER_ENDPOINT} room.RoomService/ChildOperation

Response contents:
{
  
}
```

### ParentOperation
```bash
// rpc ParentOperation(ParentOperationRequest) returns (stream Operation) {};
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"RoomId":"roomId"}' ${SERVER_ENDPOINT} room.RoomService/ParentOperation


Response contents:
{
  "RoomId": "roomId",
  "strength": 0.1
}

...
2 seconds
...

Response contents:
{
  "RoomId": "roomId",
  "strength": 0.1
}

...
2 seconds
...
```

## ssh鍵作成

`$ ssh-keygen -t rsa -f my-ssh-key -C [任意のsshユーザーネーム]`

## 設定すべき環境変数
CI/CDに必要

- GOOGLE_PROJECT_ID
- GOOGLE_COMPUTE_REGION

    例: asia-northeast1

- GOOGLE_COMPUTE_ZONE

    例: asia-northeast1-a

- GOOGLE_SERVICE_KEY

    サービスアカンウトをbase64エンコードした文字列
    `base64 -i [.json file path]`

- SSH_USERNAME

    sshユーザーネーム

- SSH_KEY

    ssh秘密鍵をbase64エンコードした文字列
    `base64 -i my-ssh-key`

- SSH_KEY_PUB

    ssh公開鍵をbase64エンコードした文字列
    `base64 -i my-ssh-key.pub`

- SSH_HOST

    ssh接続するGCEサーバーの外部IP
    terraformでインスタンス作成した後判明する外部IP

- SSH_PORT

    ssh接続のポート
    空いていればなんでもよい。基本は22だがセキュリティー上変更した方がいい
