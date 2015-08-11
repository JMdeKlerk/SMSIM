using System;
using System.Net;
using System.Net.Sockets;
using Eneter.Messaging.EndPoints.TypedMessages;
using Eneter.Messaging.MessagingSystems.MessagingSystemBase;
using Eneter.Messaging.MessagingSystems.TcpMessagingSystem;
using Eneter.Messaging.EndPoints.StringMessages;

namespace ServiceExample
{
    class Program
    {
        private static IDuplexStringMessageReceiver myReceiver;

        static void Main(string[] args)
        {
            IDuplexStringMessagesFactory aReceiverFactory = new DuplexStringMessagesFactory();
            myReceiver = aReceiverFactory.CreateDuplexStringMessageReceiver();
            myReceiver.RequestReceived += OnRequestReceived;
            IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
            IDuplexInputChannel anInputChannel = aMessaging.CreateDuplexInputChannel("tcp://" + LocalIPAddress() + ":8060/");
            myReceiver.AttachDuplexInputChannel(anInputChannel);
            Console.WriteLine("Running. Addr = " + LocalIPAddress() + ":8060");
            Console.ReadLine();
            myReceiver.DetachDuplexInputChannel();
        }

        private static void OnRequestReceived(object sender, StringRequestReceivedEventArgs e)
        {
            Console.WriteLine(e.RequestMessage);
            myReceiver.SendResponseMessage(e.ResponseReceiverId, "Ack: " + e.RequestMessage);
        }

        private static string LocalIPAddress()
        {
            IPHostEntry host;
            string localIP = "";
            host = Dns.GetHostEntry(Dns.GetHostName());
            foreach (IPAddress ip in host.AddressList)
            {
                if (ip.AddressFamily == AddressFamily.InterNetwork)
                {
                    localIP = ip.ToString();
                    break;
                }
            }
            return localIP;
        }
    }
}