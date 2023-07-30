package main

import "github.com/archivekeep/archivekeep/cmd/archivekeep-gioui/home"

var MockHomeStatus = home.Model{
	LocalArchives: []home.LocalArchiveStatusViewModel{
		{
			Title: "Documents",

			TotalFiles: 1373,

			SyncStrategyCoverageEntries: []home.LocalArchiveSyncStrategyCoverageEntryViewModel{
				{
					RemoteStorageName: "Home NAS",
					Percentage:        1,
					Description:       "online\nonsite",
				},
				{
					RemoteStorageName: "Backblaze",
					Percentage:        1,
					Description:       "online\noffsite",
				},
				{
					RemoteStorageName: "SSD A",
					Percentage:        0.92,
					Description:       "offline\nonsite",
				},
				{
					RemoteStorageName: "HDD B",
					Percentage:        0.72,
					Description:       "offsite\noffline",
				},
			},
		},
		{
			Title: "Photos",

			TotalFiles:     7411,
			UntrackedFiles: 41,

			SyncStrategyCoverageEntries: []home.LocalArchiveSyncStrategyCoverageEntryViewModel{
				{
					RemoteStorageName: "Home NAS",
					Percentage:        0.99,
					Description:       "online\nonsite",
				},
				{
					RemoteStorageName: "Backblaze",
					Percentage:        0.97,
					Description:       "online\noffsite",
				},
				{
					RemoteStorageName: "SSD A",
					Percentage:        0.97,
					Description:       "offline\nonsite",
				},
				{
					RemoteStorageName: "HDD B",
					Percentage:        0.80,
					Description:       "offsite\noffline",
				},
			},
		},
		{
			Title: "E-Books",

			TotalFiles: 43,

			SyncStrategyCoverageEntries: []home.LocalArchiveSyncStrategyCoverageEntryViewModel{
				{
					RemoteStorageName: "Home NAS",
					Percentage:        1,
					Description:       "online\nonsite",
				},
				{
					RemoteStorageName: "Backblaze",
					Percentage:        1,
					Description:       "online\noffsite",
				},
				{
					RemoteStorageName: "SSD A",
					Percentage:        1,
					Description:       "offline\nonsite",
				},
				{
					RemoteStorageName: "HDD B",
					Percentage:        0.91,
					Description:       "offsite\noffline",
				},
			},
		},
		{
			Title: "Materials",

			TotalFiles: 111,

			SyncStrategyCoverageEntries: []home.LocalArchiveSyncStrategyCoverageEntryViewModel{
				{
					RemoteStorageName: "Home NAS",
					Percentage:        1,
					Description:       "online\nonsite",
				},
				{
					RemoteStorageName: "Backblaze",
					Percentage:        1,
					Description:       "online\noffsite",
				},
				{
					RemoteStorageName: "SSD A",
					Percentage:        1,
					Description:       "offline\nonsite",
				},
				{
					RemoteStorageName: "HDD B",
					Percentage:        0.97,
					Description:       "offsite\noffline",
				},
			},
		},
	},

	ConnectedExternalStorageStatus: []home.ExternalStorageStatus{
		{
			Name: "Cloud",
		},
		{
			Name: "HDD A",
		},
		{
			Name: "SD card B",
		},
	},

	DisconnectedExternalStorageStatus: []home.ExternalStorageStatus{
		{
			Name: "Home NAS",
		},
		{
			Name: "HDD B",
		},
		{
			Name: "SD card A",
		},
		{
			Name: "SSD m.2 01",
		},
	},
}
