package home

import (
	"gioui.org/widget"
	"golang.org/x/exp/shiny/materialdesign/icons"
)

var (
	iconAdd              = mustNewIcon(icons.ContentAdd)
	iconDeviceStorage    = mustNewIcon(icons.DeviceStorage)
	iconDeviceDevices    = mustNewIcon(icons.HardwareDevicesOther)
	iconDeviceSDStorage  = mustNewIcon(icons.DeviceSDStorage)
	iconDeviceUSB        = mustNewIcon(icons.DeviceUSB)
	iconHardwareComputer = mustNewIcon(icons.HardwareComputer)
	iconSync             = mustNewIcon(icons.FileFileUpload)
)

func mustNewIcon(b []byte) *widget.Icon {
	icon, err := widget.NewIcon(b)
	if err != nil {
		panic(err)
	}
	return icon
}
