FROM golang:1.13 as builder

ENV PORT 8080

ENV CGO_ENABLED=0
ENV GOOS=linux
ENV GOARCH=amd64

ARG PACKAGE_REPOSITORY=${PACKAGE_REPOSITORY:-https://github.com/CA21engineer/HouseHackathonUnityServer/reverseProxyServer}
WORKDIR /go/src/${PACKAGE_REPOSITORY}
COPY . .
RUN go get ./... && \
    go build -o app && \
    mv app /app && \
    mv dist /dist

FROM scratch
# 軽量のalpineには必要ファイルがないため注意
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=builder /usr/share/zoneinfo /usr/share/zoneinfo
COPY --from=builder /app /app
COPY --from=builder /dist /dist
EXPOSE ${PORT}
ENTRYPOINT ["/app"]
