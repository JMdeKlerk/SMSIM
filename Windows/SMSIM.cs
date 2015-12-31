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
        private static Object threadLock = new Object();
        private IDuplexStringMessageReceiver receiver;
        private String connectedDevice;
        private bool ping = false;

        public static int smsId = 0;
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
            pingTimer.Interval = 1000 * 30;
            pingTimer.Enabled = true;
        }

        public Boolean sendMessage(String message) {
            lock(threadLock) {
                try {
                    int messageId = id++;
                    if (id > 99) id = 0;
                    message = messageId + ":" + message;
                    receiver.SendResponseMessage(connectedDevice, message);
                    return true;
                } catch (InvalidOperationException) {
                    return false;
                }
            }
        }

        private void handleRequest(object sender, StringRequestReceivedEventArgs e) {
            ping = true;
            String[] input = e.RequestMessage.Split(':');
            if (input[0].Equals("Version")) {
                connectedDevice = e.ResponseReceiverId;
                sendMessage("Version:1.1");
                connectedDevice = null;
                return;
            }
            try {
                receiver.SendResponseMessage(e.ResponseReceiverId, "Ack:" + input[0]);
            } catch (InvalidOperationException) { input[1] = "DC"; }
            switch (input[1]) {
                case ("Mismatch"):
                    var result = MessageBox.Show("An update is required. Would you like to download it now?",
                        "Update required", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                    if (result == DialogResult.Yes) System.Diagnostics.Process.Start("http://github.com/JMdeKlerk/SMSIM/releases");
                    break;
                case ("Conn"):
                    connectedDevice = e.ResponseReceiverId;
                    deviceName.Invoke(new MethodInvoker(delegate { deviceName.Text = input[2]; }));
                    contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Clear(); }));
                    sendMessage("Contacts");
                    break;
                case ("Contact"):
                    Contact contact = new Contact();
                    contact.name = input[2];
                    contact.number = input[3];
                    foreach (Contact existingContact in contacts.Items) {
                        if (existingContact.name.Equals(contact.name) && existingContact.number.Equals(contact.number)) return;
                    }
                    System.Reflection.Assembly assembly = System.Reflection.Assembly.GetExecutingAssembly();
                    Array lol = assembly.GetManifestResourceNames();
                    Stream stream = assembly.GetManifestResourceStream("SMSIM.Contacts-50.png");
                    contact.displayPic = new Bitmap(stream);
                    contacts.Invoke(new MethodInvoker(delegate {
                        contacts.Items.Add(contact);
                        contacts.Sorted = false;
                        contacts.Sorted = true;
                    }));
                    break;
                case ("SMS"):
                    SystemSounds.Beep.Play();
                    if (openConversations.ContainsKey(input[2])) {
                        Conversation conversation;
                        if (openConversations.TryGetValue(input[2], out conversation)) {
                            if (conversation.InvokeRequired)
                                conversation.Invoke(new MethodInvoker(delegate {
                                    conversation.ParseInput(input);
                                }));
                            else conversation.ParseInput(input);
                        }
                    } else {
                        BackgroundWorker bw = new BackgroundWorker();
                        bw.DoWork += new DoWorkEventHandler(delegate (object o, DoWorkEventArgs args) {
                            Conversation conversation = new Conversation(this, input);
                            openConversations.Add(input[2], conversation);
                            Application.Run(conversation);
                        });
                        bw.RunWorkerAsync();
                    }
                    break;
                case ("Success"):
                    foreach (KeyValuePair<string, Conversation> entry in openConversations) {
                        int id = Int32.Parse(input[2]);
                        entry.Value.messageSuccess(id);
                    }
                    break;
                case ("Fail"):
                    foreach (KeyValuePair<string, Conversation> entry in openConversations) {
                        int id = Int32.Parse(input[2]);
                        entry.Value.messageFail(id);
                    }
                    break;
                case ("DC"):
                    connectedDevice = null;
                    deviceName.Invoke(new MethodInvoker(delegate { deviceName.Text = "-"; }));
                    contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Clear(); }));
                    break;
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
                String[] input = { "null", "null", selected.name, selected.number };
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
                var result = MessageBox.Show("A device is currently connected. Are you sure you wish to quit?",
                    "Confirm quit", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
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
