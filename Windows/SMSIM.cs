using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Drawing;
using System.IO;
using System.Media;
using System.Net;
using System.Net.Sockets;
using System.Windows.Forms;

using Eneter.Messaging.EndPoints.StringMessages;
using Eneter.Messaging.MessagingSystems.MessagingSystemBase;
using Eneter.Messaging.MessagingSystems.TcpMessagingSystem;
using System.Timers;
using System.Threading;

namespace SMSIM {
    public partial class SMSIM : Form {

        private static int id = 0;
        private IDuplexStringMessageReceiver receiver;
        private String connectedDevice;
        private bool ping = false;
        public Dictionary<String, Conversation> openConversations = new Dictionary<string, Conversation>();

        public SMSIM() {
            InitializeComponent();
        }

        public class Contact {
            public String name { get; set; }
            public String number { get; set; }
            public Bitmap displayPic { get; set; }
        }

        private void SMSIM_Load(object sender, EventArgs e) {
            IDuplexStringMessagesFactory aReceiverFactory = new DuplexStringMessagesFactory();
            receiver = aReceiverFactory.CreateDuplexStringMessageReceiver();
            receiver.RequestReceived += handleRequest;
            IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
            String localIP = LocalIPAddress();
            IDuplexInputChannel anInputChannel = aMessaging.CreateDuplexInputChannel("tcp://" + localIP + ":8060/");
            receiver.AttachDuplexInputChannel(anInputChannel);
            if (receiver.IsDuplexInputChannelAttached) ipAddress.Text = localIP;
            this.ActiveControl = label1;
            System.Timers.Timer pingTimer = new System.Timers.Timer();
            pingTimer.Elapsed += new ElapsedEventHandler(pingTimeout);
            pingTimer.Interval = 1000 * 60 * 10;
            pingTimer.Enabled = true;
        }

        public Boolean sendMessage(String message) {
            try {
                message = Interlocked.Increment(ref id).ToString() + ":" + message;
                receiver.SendResponseMessage(connectedDevice, message);
                return true;
            } catch (InvalidOperationException) {
                return false;
            }
        }

        private void handleRequest(object sender, StringRequestReceivedEventArgs e) {
            ping = true;
            String[] input = e.RequestMessage.Split(':');
            if (input[0].Equals("Version")) {
                connectedDevice = e.ResponseReceiverId;
                sendMessage("Version:1.0");
                connectedDevice = null;
            }
            if (input[0].Equals("Conn")) {
                connectedDevice = e.ResponseReceiverId;
                deviceName.Invoke(new MethodInvoker(delegate { deviceName.Text = input[1]; }));
                contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Clear(); }));
                sendMessage("Contacts");
            }
            if (input[0].Equals("Contact")) {
                Contact contact = new Contact();
                contact.name = input[1];
                contact.number = input[2];
                foreach (Contact existingContact in contacts.Items) {
                    if (existingContact.name.Equals(contact.name) && existingContact.number.Equals(contact.number)) return;
                }
                System.Reflection.Assembly assembly = System.Reflection.Assembly.GetExecutingAssembly();
                Array lol = assembly.GetManifestResourceNames();
                Stream stream = assembly.GetManifestResourceStream("SMSIM.Contacts-50.png");
                contact.displayPic = new Bitmap(stream);
                contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Add(contact); contacts.Sorted = false; contacts.Sorted = true; }));
            }
            if (input[0].Equals("SMS")) {
                SystemSounds.Beep.Play();
                if (openConversations.ContainsKey(input[1])) {
                    Conversation conversation;
                    if (openConversations.TryGetValue(input[1], out conversation)) {
                        if (conversation.InvokeRequired) conversation.Invoke(new MethodInvoker(delegate { conversation.ParseInput(input); }));
                        else conversation.ParseInput(input);
                    }
                } else {
                    BackgroundWorker bw = new BackgroundWorker();
                    bw.DoWork += new DoWorkEventHandler(delegate (object o, DoWorkEventArgs args) {
                        Conversation conversation = new Conversation(this, input);
                        openConversations.Add(input[1], conversation);
                        Application.Run(conversation);
                    });
                    bw.RunWorkerAsync();
                }
            }
            if (input[0].Equals("DC")) {
                connectedDevice = null;
                deviceName.Invoke(new MethodInvoker(delegate { deviceName.Text = "-"; }));
                contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Clear(); }));
            }
        }

        private void pingTimeout(object source, ElapsedEventArgs e) {
            if (!ping) {
                connectedDevice = null;
                deviceName.Invoke(new MethodInvoker(delegate { deviceName.Text = "-"; }));
                contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Clear(); }));
            }
            ping = false;
            if (connectedDevice != null) sendMessage("Ping");
        }

        private void contacts_doubleClick(object sender, MouseEventArgs e) {
            Contact selected = (Contact)contacts.SelectedItem;
            if (selected == null) return;
            if (openConversations.ContainsKey(selected.name)) {
                Conversation conversation;
                if (openConversations.TryGetValue(selected.name, out conversation)) {
                    if (conversation.InvokeRequired) conversation.Invoke(new MethodInvoker(delegate { conversation.Focus(); }));
                    else conversation.Focus();
                }
            } else {
                String[] input = { "null", selected.name, selected.number };
                Conversation conversation = new Conversation(this, input);
                openConversations.Add(selected.name, conversation);
                conversation.Show();
            }
        }

        private void SMSIM_Resize(object sender, EventArgs e) {
            if (FormWindowState.Minimized == this.WindowState) {
                this.Hide();
            }
        }

        private void trayIcon_MouseDoubleClick(object sender, MouseEventArgs e) {
            this.Show();
            this.WindowState = FormWindowState.Normal;
        }

        private void SMSIM_FormClosing(object sender, FormClosingEventArgs e) {
            if (connectedDevice != null) {
                var result = MessageBox.Show("A device is currently connected. Are you sure you wish to quit?", "Confirm Quit", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (result == DialogResult.No) e.Cancel = true;
                else {
                    sendMessage("DC");
                    receiver.DetachDuplexInputChannel();
                }
            } else receiver.DetachDuplexInputChannel();
        }

        private string LocalIPAddress() {
            IPHostEntry host;
            string localIP = "";
            host = Dns.GetHostEntry(Dns.GetHostName());
            foreach (IPAddress ip in host.AddressList) {
                if (ip.AddressFamily == AddressFamily.InterNetwork) {
                    localIP = ip.ToString();
                    break;
                }
            }
            return localIP;
        }
    }
}
