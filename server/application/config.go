package application

import (
	"fmt"
	"net"
)

type Config struct {
	DataPath string

	GRPC GRPCConfig
	HTTP HTTPConfig
}

type GRPCConfig struct {
	Enable bool

	Port uint16
}

type HTTPConfig struct {
	Enable bool

	Port       uint16
	PublicPath string
}

type ServerListeners struct {
	GRPC struct {
		Listeners []net.Listener
	}
	HTTP struct {
		Listeners []net.Listener
	}
}

func (c Config) Listen() (ServerListeners, error) {
	listeners := &ServerListeners{}

	if c.HTTP.Enable {
		ln, err := net.Listen("tcp", fmt.Sprintf(":%d", c.HTTP.Port))
		if err != nil {
			return *listeners, fmt.Errorf("listen HTTP: %w", err)
		}

		listeners.HTTP.Listeners = append(listeners.HTTP.Listeners, ln)
	}

	if c.GRPC.Enable {
		ln, err := net.Listen("tcp", fmt.Sprintf(":%d", c.GRPC.Port))
		if err != nil {
			return *listeners, fmt.Errorf("listen HTTP: %w", err)
		}

		listeners.GRPC.Listeners = append(listeners.GRPC.Listeners, ln)
	}

	return *listeners, nil
}
