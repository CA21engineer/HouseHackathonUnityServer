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

### ゲーム開始まで
1. 親が部屋作成
```bash
// Terminal 1
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"AccountId":"parent","AccountName":"parentName", "roomKey":""}' ${SERVER_ENDPOINT} room.RoomService/CreateRoom
```

2. 子が3人はいる
```bash
// Terminal 2
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"AccountId":"child1", "AccountName":"childName1","roomKey":""}' ${SERVER_ENDPOINT} room.RoomService/JoinRoom

// Terminal 3
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"AccountId":"child2","AccountName":"childName2","roomKey":""}' ${SERVER_ENDPOINT} room.RoomService/JoinRoom

// Terminal 4
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"AccountId":"child3","AccountName":"childName3","roomKey":""}' ${SERVER_ENDPOINT} room.RoomService/JoinRoom
```

- 人が入るたびにルームIdと後何人入れるかが、すでに入ってる全員に送られる
```bash
Response contents:
{
  "joinRoomResponse": {
    "RoomId": "dfa65e98a18340cbb77a4fb9738d9a16",
    "vagrant": 1
  }
}
```

- 全員入集まると以下データが全員に送られ、このコネクションは閉じられる
```bash
Response contents:
{
  "readyResponse": {
    "RoomId": "dfa65e98a18340cbb77a4fb9738d9a16",
    "ghostRecord": [
        {
            "x": 0.1,
            "y": 0.1,
            "date": 0
        }
    ],
    "Member": [
      {
        "AccountName": "parentName",
        "Direction": "Down"
      },
      {
        "AccountName": "childName1",
        "Direction": "Right"
      },
      {
        "AccountName": "childName2",
        "Direction": "Left"
      },
      {
        "AccountName": "childName3",
        "Direction": "Up"
      },
    ],
    "Direction": "Right",// あなたの操作方向
    "date": "2020-05-04T03:49:32.583Z"
  }
}
```

3. ゲーム中に使用するコネクションを開く
- CoordinateSharing
親は自機の演算結果の球の座標を送信する。
子は座標を受け取り自機に反映する。

```bash
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"x":0,"y":0,"z":0,"date":0}' -H 'roomid: dfa65e98a18340cbb77a4fb9738d9a16' -H 'accountid: parent' ${SERVER_ENDPOINT} room.RoomService/CoordinateSharing

$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -H 'roomid: dfa65e98a18340cbb77a4fb9738d9a16' -H 'accountid: child1' ${SERVER_ENDPOINT} room.RoomService/CoordinateSharing
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -H 'roomid: dfa65e98a18340cbb77a4fb9738d9a16' -H 'accountid: child2' ${SERVER_ENDPOINT} room.RoomService/CoordinateSharing
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -H 'roomid: dfa65e98a18340cbb77a4fb9738d9a16' -H 'accountid: child3' ${SERVER_ENDPOINT} room.RoomService/CoordinateSharing
```

- ChildOperation
子は操作方向と操作量を送る

```bash
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"Direction":"Right","strength":0.12345}' -H 'roomid: dfa65e98a18340cbb77a4fb9738d9a16' -H 'accountid: child1' ${SERVER_ENDPOINT} room.RoomService/ChildOperation
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"Direction":"Left","strength":0.12345}' -H 'roomid: dfa65e98a18340cbb77a4fb9738d9a16' -H 'accountid: child2' ${SERVER_ENDPOINT} room.RoomService/ChildOperation
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"Direction":"Up","strength":0.12345}' -H 'roomid: dfa65e98a18340cbb77a4fb9738d9a16' -H 'accountid: child3' ${SERVER_ENDPOINT} room.RoomService/ChildOperation
```
- ParentOperation
親は子の操作状況を受け取る
```bash
$ grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"RoomId":"dfa65e98a18340cbb77a4fb9738d9a16","AccountId":"parent"}' ${SERVER_ENDPOINT} room.RoomService/ParentOperation
```

4. 結果の送信

親がゲーム結果を送信する

```bash
grpcurl -v -plaintext -import-path . -proto apiServer/src/main/protobuf/room.proto -d '{"RoomId":"dfa65e98a18340cbb77a4fb9738d9a16","AccountId":"parent","ghostRecord":[{"x":0.1,"y":0.1,"z":0.1,"date":0}],"isGameClear":true,"date":10}' ${SERVER_ENDPOINT} room.RoomService/SendResult
```

## ws
```bash
$ wscat -c ws://localhost:18080/health


$ wscat -c "ws://localhost:18080/create_room?accountId=parent&accountName=parentName"
> {"":""}
```

```bash
$ wscat -c "ws://localhost:18080/join_room?accountId=child1&accountName=child1Name"
> {"direction":{"direction":"Up"}, "strength":0.1}

$ wscat -c "ws://localhost:18080/join_room?accountId=child2&accountName=child2Name"
$ wscat -c "ws://localhost:18080/join_room?accountId=child3&accountName=child3Name"


```

## C# code gen
```bash
$ chmod +x ./grpc-code-gen/code-gen-c#.sh
$ ./grpc-code-gen/code-gen-c#.sh ./apiServer/src/main/protobuf ./pb
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
