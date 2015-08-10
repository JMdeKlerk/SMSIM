using System;
using Eneter.Messaging.EndPoints.TypedMessages;
using Eneter.Messaging.MessagingSystems.MessagingSystemBase;
using Eneter.Messaging.MessagingSystems.TcpMessagingSystem;
using System.Net;
using System.Net.Sockets;

namespace ServiceExample
{
    public class MyRequest
    {
        public string Text { get; set; }
    }
    public class MyResponse
    {
        public int Length { get; set; }
    }

    class Program
    {
        private static IDuplexTypedMessageReceiver<MyResponse, MyRequest> myReceiver;

        static void Main(string[] args)
        {
            IDuplexTypedMessagesFactory aReceiverFactory = new DuplexTypedMessagesFactory();
            myReceiver = aReceiverFactory.CreateDuplexTypedMessageReceiver<MyResponse, MyRequest>();
            myReceiver.MessageReceived += OnMessageReceived;
            IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
            IDuplexInputChannel anInputChannel = aMessaging.CreateDuplexInputChannel("tcp://" + LocalIPAddress() + ":8060/");
            myReceiver.AttachDuplexInputChannel(anInputChannel);
            Console.WriteLine("The service is running. To stop press enter.");
            Console.ReadLine();
            myReceiver.DetachDuplexInputChannel();
        }

        private static void OnMessageReceived(object sender, TypedRequestReceivedEventArgs<MyRequest> e)
        {
            Console.WriteLine("Received: " + e.RequestMessage.Text);
            MyResponse aResponse = new MyResponse();
            aResponse.Length = e.RequestMessage.Text.Length;
            myReceiver.SendResponseMessage(e.ResponseReceiverId, aResponse);
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