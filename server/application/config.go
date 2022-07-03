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

	ListenOnAllInterfaces bool
}

type HTTPConfig struct {
	Enable bool

	Port       uint16
	PublicPath string

	ListenOnAllInterfaces bool
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
		lis, err := openListeners(c.HTTP.ListenOnAllInterfaces, c.HTTP.Port)
		if err != nil {
			return *listeners, fmt.Errorf("open HTTP listeners: %w", err)
		}

		listeners.HTTP.Listeners = lis
	}

	if c.GRPC.Enable {
		lis, err := openListeners(c.GRPC.ListenOnAllInterfaces, c.GRPC.Port)
		if err != nil {
			return *listeners, fmt.Errorf("open GRPC listeners: %w", err)
		}

		listeners.GRPC.Listeners = lis
	}

	return *listeners, nil
}

func openListeners(listenOnAllInterfaces bool, port uint16) ([]net.Listener, error) {
	var addrs []string

	if listenOnAllInterfaces {
		ipAddrs, err := getAddrs()
		if err != nil {
			return nil, fmt.Errorf("get addresses of machine interfaces: %w", err)
		}
		for _, ip := range ipAddrs {
			addrs = append(addrs, fmt.Sprintf("%s:%d", ip.String(), port))
		}
	} else {
		addrs = []string{fmt.Sprintf(":%d", port)}
	}

	var listeners []net.Listener

	for _, addr := range addrs {
		ln, err := net.Listen("tcp", addr)
		if err != nil {
			return nil, fmt.Errorf("listen on %s: %w", addr, err)
		}
		listeners = append(listeners, ln)
	}
	return listeners, nil
}

func getAddrs() ([]net.IP, error) {
	allAddrs, err := net.InterfaceAddrs()
	if err != nil {
		return nil, err
	}

	var addrs []net.IP

	for _, addr := range allAddrs {
		var ip net.IP
		switch v := addr.(type) {
		case *net.IPNet:
			ip = v.IP
		case *net.IPAddr:
			ip = v.IP
		}

		if ipv4 := ip.To4(); ipv4 != nil {
			addrs = append(addrs, ipv4)
		}
	}

	return addrs, nil
}
