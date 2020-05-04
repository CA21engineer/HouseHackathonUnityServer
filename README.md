# HouseHackathonUnityServer

## 資料
- [議事録](https://github.com/CA21engineer/HouseHackathonUnityClient/issues/1)

## APIServer情報
- Host: 35.243.119.173
- Port: 18080

## 動作確認
※モックを組み込んでいるので本番とはレスポンスが違う

```bash
$ export SERVER_ENDPOINT=localhost:18080
or
$ export SERVER_ENDPOINT=35.243.119.173:18080
```

1. build, run
※ローカルでサーバー起動するときのみ

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
    "RoomId": "roomId"
  }
}
...
2 seconds
...
Response contents:
{
  "joinRoomResponse": {
    "RoomId": "roomId"
  }
}
...
2 seconds
...
Response contents:
{
  "readyResponse": {
    "Member": [
      {
        "AccountId": "user1"
      },
      {
        "AccountId": "user2"
      },
      {
        "AccountId": "user3"
      },
      {
        "AccountId": "user4"
      }
    ],
    "date": "2020-05-03T12:37:14.010Z"
  }
}

```

### JoinRoom
```bash
// rpc JoinRoom(JoinRoomRequest) returns (stream RoomResponse) {};
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"AccountId":"bambootuna","roomKey":""}' ${SERVER_ENDPOINT} room.RoomService/JoinRoom

Response contents:
{
  "createRoomResponse": {
    "RoomId": "roomId"
  }
}
...
2 seconds
...
Response contents:
{
  "joinRoomResponse": {
    "RoomId": "roomId"
  }
}
...
2 seconds
...
Response contents:
{
  "readyResponse": {
    "Member": [
      {
        "AccountId": "user1"
      },
      {
        "AccountId": "user2"
      },
      {
        "AccountId": "user3"
      },
      {
        "AccountId": "user4"
      }
    ],
    "date": "2020-05-03T12:38:41.522Z"
  }
}

```

### CoordinateSharing
```bash
// rpc CoordinateSharing(stream Coordinate) returns (stream Coordinate) {};
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"x":0,"y":0,"date":"2020-05-03T03:57:57.812Z"}' -H 'roomid: roomId' -H 'accountid: accountId' ${SERVER_ENDPOINT} room.RoomService/CoordinateSharing

Response contents:
{
  "date": "2020-05-03T03:57:57.812Z"
}

```

### ChildOperation
```bash
// rpc ChildOperation(stream Operation) returns (Empty) {};
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"Direction":0,"strength":0.12345}' -H 'roomid: roomId' -H 'accountid: accountId' ${SERVER_ENDPOINT} room.RoomService/ChildOperation

Response contents:
{
  
}
```

### ParentOperation
```bash
// rpc ParentOperation(ParentOperationRequest) returns (stream Operation) {};
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"RoomId":"roomId","AccountId":"accountId"}' ${SERVER_ENDPOINT} room.RoomService/ParentOperation


Response contents:
{
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
