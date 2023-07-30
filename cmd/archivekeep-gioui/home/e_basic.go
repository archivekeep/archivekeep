package home

import (
	"image"
	"image/color"

	"gioui.org/layout"
	"gioui.org/op/clip"
	"gioui.org/op/paint"
	"gioui.org/unit"
)

func horizontalLine(color color.NRGBA) layout.Widget {
	return func(gtx layout.Context) layout.Dimensions {
		defer clip.Rect(image.Rect(0, 0, gtx.Constraints.Max.X, 1)).Push(gtx.Ops).Pop()
		paint.ColorOp{Color: color}.Add(gtx.Ops)
		paint.PaintOp{}.Add(gtx.Ops)

		return layout.Dimensions{
			Size:     image.Pt(20, 1),
			Baseline: 0,
		}
	}
}

func horizontalSpacer(width unit.Dp) layout.Widget {
	return layout.Spacer{Width: width}.Layout
}

func verticalSpacer(height unit.Dp) layout.Widget {
	return layout.Spacer{Height: height}.Layout
}
