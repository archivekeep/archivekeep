package home

import (
	_ "embed"
	"fmt"
	"image"
	"image/color"

	"gioui.org/layout"
	"gioui.org/op/clip"
	"gioui.org/op/paint"
	"gioui.org/text"
	"gioui.org/unit"
	"gioui.org/widget"
	"gioui.org/widget/material"
)

var (
	sectionTitleColor      = color.NRGBA{R: 111, G: 111, B: 111, A: 255}
	secondaryMetadataColor = color.NRGBA{R: 68, G: 68, B: 68, A: 255}

	listSeparatorColor = color.NRGBA{R: 211, G: 211, B: 211, A: 255}

	trackItemsFontSize        unit.Sp = 14
	trackItemsButtonTextColor         = color.NRGBA{R: 252, G: 252, B: 252, A: 255}
)

type View struct {
	Theme *material.Theme

	Content Model

	mainContentList widget.List
}

func (u *View) Layout(gtx layout.Context) {
	u.mainContentList.List.Axis = layout.Vertical

	material.List(u.Theme, &u.mainContentList).Layout(gtx, 1, func(gtx layout.Context, index int) layout.Dimensions {
		if index == 0 {
			return u.mainContent(gtx)
		} else {
			return layout.Spacer{Width: 1, Height: 1}.Layout(gtx)
		}
	})
}

func (u *View) mainContent(gtx layout.Context) layout.Dimensions {
	return layout.Inset{
		Top:    12,
		Bottom: 12,
		Left:   12,
		Right:  6,
	}.Layout(gtx, func(gtx layout.Context) layout.Dimensions {
		return layout.Flex{Axis: layout.Horizontal}.Layout(gtx,
			layout.Flexed(5, func(gtx layout.Context) layout.Dimensions {
				return layout.Flex{Axis: layout.Vertical}.Layout(
					gtx,
					layout.Rigid(u.localArchivesSection),
					layout.Rigid(u.connectedArchivesSection),
					layout.Rigid(u.disconnectedStoragesSection),
				)
			}),
			layout.Rigid(horizontalSpacer(12)),
			layout.Flexed(2, func(gtx layout.Context) layout.Dimensions {
				return layout.Flex{Axis: layout.Vertical}.Layout(
					gtx,
					layout.Rigid(u.thisDeviceSection),
					layout.Rigid(u.otherSection),
				)
			}),
		)
	})
}

func (u *View) localArchivesSection(gtx layout.Context) layout.Dimensions {
	return renderSection(
		gtx,
		u.Theme,
		iconHardwareComputer,
		"Local archives", func(gtx layout.Context) layout.Dimensions {
			var widgets []layout.FlexChild

			for idx, _localArchive := range u.Content.LocalArchives {
				localArchive := _localArchive

				if idx > 0 {
					widgets = append(widgets, layout.Rigid(verticalSpacer(12)))
					widgets = append(widgets, layout.Rigid(horizontalLine(listSeparatorColor)))
					widgets = append(widgets, layout.Rigid(verticalSpacer(12)))
				}

				widgets = append(widgets, layout.Rigid(func(gtx layout.Context) layout.Dimensions {
					return layout.Flex{Axis: layout.Horizontal, Alignment: layout.Start}.Layout(
						gtx,
						layout.Rigid(func(gtx layout.Context) layout.Dimensions {
							widgets := []layout.FlexChild{
								layout.Rigid(archiveTitle(u.Theme, localArchive.Title).Layout),
								layout.Rigid(archiveMetadataText(u.Theme, fmt.Sprintf("%d files", localArchive.TotalFiles)).Layout),
							}

							if localArchive.UntrackedFiles > 0 {
								c := widget.Clickable{}

								widgets = append(widgets, layout.Rigid(archiveMetadataText(u.Theme, fmt.Sprintf("%d untracked files", localArchive.UntrackedFiles)).Layout))
								widgets = append(widgets, layout.Rigid(verticalSpacer(4)))
								widgets = append(widgets, layout.Rigid(func(gtx layout.Context) layout.Dimensions {
									return material.ButtonLayout(u.Theme, &c).Layout(gtx, func(gtx layout.Context) layout.Dimensions {
										return layout.UniformInset(6).Layout(gtx, func(gtx layout.Context) layout.Dimensions {
											title := material.Label(u.Theme, u.Theme.TextSize*trackItemsFontSize/16, "track new files")
											title.Color = trackItemsButtonTextColor

											return layout.Flex{Axis: layout.Horizontal, Alignment: layout.Middle}.Layout(
												gtx,
												layout.Rigid(func(_gtx layout.Context) layout.Dimensions {
													gtx := gtx
													gtx.Constraints = layout.Constraints{Max: image.Pt(gtx.Sp(trackItemsFontSize), gtx.Sp(trackItemsFontSize))}
													return iconAdd.Layout(gtx, trackItemsButtonTextColor)
												}),
												layout.Rigid(horizontalSpacer(2)),
												layout.Rigid(title.Layout),
											)
										})
									})
								}))
							}

							return layout.Flex{Axis: layout.Vertical}.Layout(gtx, widgets...)
						}),
						layout.Flexed(1, layout.Spacer{}.Layout),
						layout.Rigid(func(gtx layout.Context) layout.Dimensions {
							items := []layout.FlexChild{}

							for idx, _coverageEntry := range localArchive.SyncStrategyCoverageEntries {
								coverageEntry := _coverageEntry

								items = append(items, layout.Rigid(
									externalReplicaState(
										u.Theme,
										idx == len(localArchive.SyncStrategyCoverageEntries)-1,
										coverageEntry.Percentage,
										coverageEntry.Description,
										coverageEntry.RemoteStorageName,
									),
								))
							}

							return layout.Flex{Axis: layout.Horizontal}.Layout(gtx, items...)
						}),
					)
				}))
			}

			return layout.Flex{
				Axis: layout.Vertical,
			}.Layout(gtx, widgets...)
		},
	)
}

func (u *View) connectedArchivesSection(gtx layout.Context) layout.Dimensions {
	return renderSection(
		gtx,
		u.Theme,
		iconDeviceUSB,
		"Connected external storages",
		func(gtx layout.Context) layout.Dimensions {
			var widgets []layout.FlexChild

			for idx, _externalStorageStatus := range u.Content.ConnectedExternalStorageStatus {
				externalStorageStatus := _externalStorageStatus

				if idx > 0 {
					widgets = append(widgets, layout.Rigid(verticalSpacer(12)))
					widgets = append(widgets, layout.Rigid(horizontalLine(listSeparatorColor)))
					widgets = append(widgets, layout.Rigid(verticalSpacer(12)))
				}

				widgets = append(widgets, layout.Rigid(
					func(gtx layout.Context) layout.Dimensions {
						return layout.Flex{Axis: layout.Horizontal}.Layout(
							gtx,
							layout.Flexed(1, externalStorageTitle(u.Theme, externalStorageStatus.Name).Layout),
							layout.Rigid(func(gtx layout.Context) layout.Dimensions {
								return layout.Flex{Axis: layout.Horizontal}.Layout(
									gtx,
									layout.Rigid(externalStorageArchiveStat(u.Theme, false, "Documents", 1452, 1543)),
									layout.Rigid(externalStorageArchiveStat(u.Theme, false, "Photos", 5642, 5923)),
									layout.Rigid(externalStorageArchiveStat(u.Theme, false, "E-books", 56, 56)),
									layout.Rigid(externalStorageArchiveStat(u.Theme, true, "Materials", 142, 142)),
								)
							}),
						)
					},
				))
			}

			return layout.Flex{Axis: layout.Vertical}.Layout(gtx, widgets...)
		},
	)
}

func (u *View) disconnectedStoragesSection(gtx layout.Context) layout.Dimensions {
	return renderSection(
		gtx,
		u.Theme,
		iconDeviceSDStorage,
		"Disconnected external storages",
		emptySectionState(u.Theme, "To be continued ..."),
	)
}

func (u *View) thisDeviceSection(gtx layout.Context) layout.Dimensions {
	return renderSection(
		gtx,
		u.Theme,
		iconDeviceStorage,
		"This device",
		func(gtx layout.Context) layout.Dimensions {
			return layout.Flex{Axis: layout.Vertical}.Layout(
				gtx,
				statTableEntry(u.Theme, "Total size", "12.1 GB"),
				statTableEntry(u.Theme, "Total files", "1231"),
			)
		},
	)
}

func (u *View) otherSection(gtx layout.Context) layout.Dimensions {
	return renderSection(
		gtx,
		u.Theme,
		iconDeviceDevices,
		"Other info or controls",
		emptySectionState(u.Theme, "To be continued ..."),
	)
}

func statTableEntry(th *material.Theme, label string, value string) layout.FlexChild {
	return layout.Rigid(func(gtx layout.Context) layout.Dimensions {
		return layout.Inset{
			Top:    2,
			Bottom: 2,
			Left:   0,
			Right:  0,
		}.Layout(gtx, func(gtx layout.Context) layout.Dimensions {
			return layout.Flex{Axis: layout.Horizontal}.Layout(
				gtx,
				layout.Rigid(statTitle(th, label).Layout),
				layout.Flexed(1, statValue(th, value).Layout),
			)
		})
	})
}

func archiveTitle(th *material.Theme, txt string) material.LabelStyle {
	label := material.Label(th, th.TextSize*20/16, txt)
	label.Font.Weight = 700
	label.Color = color.NRGBA{R: 66, G: 66, B: 66, A: 255}

	return label
}

func archiveMetadataText(th *material.Theme, txt string) material.LabelStyle {
	label := material.Label(th, th.TextSize*12/16, txt)
	label.Color = secondaryMetadataColor

	return label
}

func externalReplicaState(th *material.Theme, last bool, percent float64, description, name string) layout.Widget {
	return func(gtx layout.Context) layout.Dimensions {
		labelPercent := material.Label(th, th.TextSize*20/16, fmt.Sprintf("%.0f%%", percent*100))
		labelPercent.Alignment = text.Middle
		labelPercent.Font.Weight = 700
		labelName := material.Label(th, th.TextSize*12/16, name)
		labelName.Alignment = text.Middle
		labelName.Font.Weight = 700
		labelDescription := material.Label(th, th.TextSize*10/16, description)
		labelDescription.Alignment = text.Middle

		size := layout.Flex{Axis: layout.Vertical, Alignment: layout.Middle}.Layout(
			gtx,
			layout.Rigid(horizontalSpacer(92)),
			layout.Rigid(labelName.Layout),
			layout.Rigid(verticalSpacer(4)),
			layout.Rigid(labelPercent.Layout),
			layout.Rigid(verticalSpacer(4)),
			layout.Rigid(labelDescription.Layout),
		)

		if !last {
			separatorColor := color.NRGBA{R: 200, G: 200, B: 200, A: 255}
			separatorMargin := unit.Dp(6)

			paint.FillShape(gtx.Ops, separatorColor, clip.Rect{
				Min: image.Point{X: size.Size.X, Y: gtx.Dp(separatorMargin)},
				Max: image.Point{X: size.Size.X + gtx.Dp(1), Y: size.Size.Y - gtx.Dp(separatorMargin)},
			}.Op())

			size.Size.X += gtx.Dp(1)
		}

		return size
	}
}

func externalStorageTitle(th *material.Theme, txt string) material.LabelStyle {
	label := material.Label(th, th.TextSize*18/16, txt)
	label.Font.Weight = 700
	return label
}

func externalStorageArchiveStat(
	th *material.Theme,
	last bool,
	archiveName string,
	filesSynced int64,
	filesTotal int64,
) layout.Widget {
	return func(gtx layout.Context) layout.Dimensions {
		percent := float64(filesSynced) / float64(filesTotal)

		labelo := material.Label(th, th.TextSize*20/16, fmt.Sprintf("%.0f%%", percent*100))
		labelo.Alignment = text.Middle
		labelo.Font.Weight = 700
		labeln := material.Label(th, th.TextSize*12/16, archiveName)
		labeln.Alignment = text.Middle
		labeln.Font.Weight = 700
		labeld := material.Label(th, th.TextSize*10/16, fmt.Sprintf("%d / %d", filesSynced, filesTotal))
		labeld.Alignment = text.Middle

		size := layout.Flex{Axis: layout.Vertical, Alignment: layout.Middle}.Layout(
			gtx,
			layout.Rigid(horizontalSpacer(92)),
			layout.Rigid(labeln.Layout),
			layout.Rigid(verticalSpacer(4)),
			layout.Rigid(labelo.Layout),
			layout.Rigid(verticalSpacer(4)),
			layout.Rigid(labeld.Layout),
			layout.Rigid(verticalSpacer(4)),
			layout.Rigid(func(gtx layout.Context) layout.Dimensions {
				c := widget.Clickable{}

				return material.ButtonLayout(th, &c).Layout(gtx, func(gtx layout.Context) layout.Dimensions {
					return layout.UniformInset(6).Layout(gtx, func(gtx layout.Context) layout.Dimensions {
						title := material.Label(th, th.TextSize*trackItemsFontSize/16, "sync")
						title.Color = trackItemsButtonTextColor

						return layout.Flex{Axis: layout.Horizontal, Alignment: layout.End}.Layout(
							gtx,
							layout.Rigid(func(_gtx layout.Context) layout.Dimensions {
								gtx := gtx
								gtx.Constraints = layout.Constraints{Max: image.Pt(gtx.Sp(trackItemsFontSize), gtx.Sp(trackItemsFontSize))}
								return iconSync.Layout(gtx, trackItemsButtonTextColor)
							}),
							layout.Rigid(horizontalSpacer(2)),
							layout.Rigid(title.Layout),
						)
					})
				})
			}),
		)

		if !last {
			separatorColor := color.NRGBA{R: 200, G: 200, B: 200, A: 255}
			separatorMargin := unit.Dp(6)

			paint.FillShape(gtx.Ops, separatorColor, clip.Rect{
				Min: image.Point{X: size.Size.X, Y: gtx.Dp(separatorMargin)},
				Max: image.Point{X: size.Size.X + gtx.Dp(1), Y: size.Size.Y - gtx.Dp(separatorMargin)},
			}.Op())

			size.Size.X += gtx.Dp(1)
		}

		return size
	}
}

func statTitle(th *material.Theme, txt string) material.LabelStyle {
	label := material.Label(th, th.TextSize*12/16, txt)
	label.Alignment = text.Start
	return label
}

func statValue(th *material.Theme, txt string) material.LabelStyle {
	label := material.Label(th, th.TextSize*12/16, txt)
	label.Alignment = text.End
	label.Font.Weight = 700
	return label
}
