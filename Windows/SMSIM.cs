using System;
using System.Net;
using System.Net.Sockets;
using System.Windows.Forms;

using Eneter.Messaging.EndPoints.StringMessages;
using Eneter.Messaging.MessagingSystems.MessagingSystemBase;
using Eneter.Messaging.MessagingSystems.TcpMessagingSystem;


namespace SMSIM
{
    public partial class SMSIM : Form
    {

        private IDuplexStringMessageReceiver myReceiver;

        public SMSIM()
        {
            InitializeComponent();
        }

        private void SMSIM_Load(object sender, EventArgs e)
        {
            IDuplexStringMessagesFactory aReceiverFactory = new DuplexStringMessagesFactory();
            myReceiver = aReceiverFactory.CreateDuplexStringMessageReceiver();
            myReceiver.RequestReceived += OnRequestReceived;
            IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
            String localIP = LocalIPAddress();
            IDuplexInputChannel anInputChannel = aMessaging.CreateDuplexInputChannel("tcp://" + localIP + ":8060/");
            myReceiver.AttachDuplexInputChannel(anInputChannel);
            ipAddress.Text = localIP + ":8060";
        }
        
        private void OnRequestReceived(object sender, StringRequestReceivedEventArgs e)
        {
            String[] input = e.RequestMessage.Split(':');
            if (input[0].Equals("Conn"))
            {
                deviceName.Invoke(new MethodInvoker(delegate { deviceName.Text = input[1]; }));
            }
            myReceiver.SendResponseMessage(e.ResponseReceiverId, "Ack: " + e.RequestMessage);
        }

        private string LocalIPAddress()
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
