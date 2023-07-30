package main

import (
	"image/color"
	"log"
	"os"

	"gioui.org/app"
	"gioui.org/font/gofont"
	"gioui.org/io/system"
	"gioui.org/layout"
	"gioui.org/op"
	"gioui.org/widget/material"

	"github.com/archivekeep/archivekeep/cmd/archivekeep-gioui/home"
)

func main() {
	go func() {
		w := app.NewWindow(
			app.Size(960, 620),
			app.MinSize(300, 300),
		)
		err := run(w)
		if err != nil {
			log.Fatal(err)
		}
		os.Exit(0)
	}()
	app.Main()
}

func run(w *app.Window) error {
	th := material.NewTheme(gofont.Collection())
	th.Fg = color.NRGBA{R: 34, G: 34, B: 34, A: 255}

	homeView := home.View{
		Theme:   th,
		Content: MockHomeStatus,
	}

	var ops op.Ops
	for {
		e := <-w.Events()
		switch e := e.(type) {
		case system.DestroyEvent:
			return e.Err
		case system.FrameEvent:
			gtx := layout.NewContext(&ops, e)

			homeView.Layout(gtx)

			e.Frame(gtx.Ops)
		}
	}
}
