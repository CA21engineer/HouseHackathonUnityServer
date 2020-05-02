# HouseHackathonUnityServer

## 資料
- [議事録](https://github.com/CA21engineer/HouseHackathonUnityClient/issues/1)

## APIServer情報
- Host: 35.243.119.173
- Port: 18080
```bash
$ curl -X GET 35.243.119.173:18080/health
OK
```

## ローカル動作確認
```bash
$ chmod +x run-local.sh
$ ./run-local.sh
...
// 初回はめっちゃ時間かかる...
$ curl -X GET localhost:18080/health
OK
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
