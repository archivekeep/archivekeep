package home

import (
	"image"
	"image/color"

	"gioui.org/layout"
	"gioui.org/text"
	"gioui.org/widget"
	"gioui.org/widget/material"
)

func renderSection(gtx layout.Context, th *material.Theme, icon *widget.Icon, title string, content layout.Widget) layout.Dimensions {
	return layout.Flex{Axis: layout.Vertical}.Layout(
		gtx,
		layout.Rigid(verticalSpacer(12)),
		layout.Rigid(sectionTitleWithIcon(th, icon, title)),
		layout.Rigid(verticalSpacer(3)),
		layout.Rigid(horizontalLine(color.NRGBA{R: 121, G: 121, B: 121, A: 255})),
		layout.Rigid(verticalSpacer(16)),
		layout.Rigid(content),
		layout.Rigid(verticalSpacer(16)),
	)
}

func sectionTitleWithIcon(th *material.Theme, icon *widget.Icon, s string) layout.Widget {
	return func(gtx layout.Context) layout.Dimensions {
		return layout.Flex{
			Axis:      layout.Horizontal,
			Alignment: layout.Middle,
		}.Layout(
			gtx,
			layout.Rigid(func(gtx layout.Context) layout.Dimensions {
				cgtx := gtx
				cgtx.Constraints = layout.Constraints{
					Min: image.Point{},
					Max: image.Pt(gtx.Sp(th.TextSize), gtx.Sp(th.TextSize)),
				}

				return icon.Layout(cgtx, sectionTitleColor)

			}),
			layout.Rigid(layout.Spacer{Width: 4}.Layout),
			layout.Rigid(sectionTitle(th, s).Layout),
		)
	}
}

func emptySectionState(theme *material.Theme, txt string) layout.Widget {
	return func(gtx layout.Context) layout.Dimensions {
		label := material.Label(theme, theme.TextSize*14/16, txt)
		label.Alignment = text.Middle

		return layout.Inset{
			Top:    4,
			Bottom: 4,
			Left:   0,
			Right:  0,
		}.Layout(gtx, label.Layout)
	}
}

func sectionTitle(th *material.Theme, txt string) material.LabelStyle {
	label := material.Label(th, th.TextSize, txt)
	label.Font.Weight = 700
	label.Color = sectionTitleColor

	return label
}
