package main

import (
	"fmt"
	"github.com/BambooTuna/go-server-lib/config"
	"github.com/gin-contrib/static"
	"github.com/gin-gonic/gin"
	"log"
)

func main() {
	serverPort := config.GetEnvString("PORT", "8080")
	r := gin.Default()

	r.Use(static.Serve("/", static.LocalFile("./dist", false)))
	r.NoRoute(func(c *gin.Context) {
		c.File("./dist/index.html")
	})

	log.Fatal(r.Run(fmt.Sprintf(":%s", serverPort)))
}
