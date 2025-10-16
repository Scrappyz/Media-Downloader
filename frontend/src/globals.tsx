// const address: string = "localhost"; // Use this for development
const address: string = "192.168.68.100"; // Enter the IPV4 address of your LAN here to expose to other devices on your network
const port: string = "8080";
export const api: string = `http://${address}:${port}/api/v1`