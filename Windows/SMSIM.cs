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

        private IDuplexStringMessageReceiver receiver;

        public SMSIM()
        {
            InitializeComponent();
        }

        private void SMSIM_Load(object sender, EventArgs e)
        {
            IDuplexStringMessagesFactory aReceiverFactory = new DuplexStringMessagesFactory();
            receiver = aReceiverFactory.CreateDuplexStringMessageReceiver();
            receiver.RequestReceived += OnRequestReceived;
            IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
            String localIP = LocalIPAddress();
            IDuplexInputChannel anInputChannel = aMessaging.CreateDuplexInputChannel("tcp://" + localIP + ":8060/");
            receiver.AttachDuplexInputChannel(anInputChannel);
            ipAddress.Text = localIP + ":8060";
            this.ActiveControl = label1;
        }
        
        private void OnRequestReceived(object sender, StringRequestReceivedEventArgs e)
        {
            receiver.SendResponseMessage(e.ResponseReceiverId, "Ack: " + e.RequestMessage);
            String[] input = e.RequestMessage.Split(':');
            if (input[0].Equals("Conn"))
            {
                deviceName.Invoke(new MethodInvoker(delegate { deviceName.Text = input[1]; }));
            }
            if (input[0].Equals("Contact"))
            {
                ListViewItem name = new ListViewItem(input[1]);
                ListViewItem.ListViewSubItem number = new ListViewItem.ListViewSubItem(name, input[2]);
                name.SubItems.Add(number);
                contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Add(name); }));
            }
            Console.WriteLine(e.RequestMessage);
        }

        private void contacts_SelectionChanged(object sender, ListViewItemSelectionChangedEventArgs e)
        {
            //contacts.FocusedItem.Text = (String) contacts.FocusedItem.Tag;
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
