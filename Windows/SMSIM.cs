﻿using System;
using System.Net;
using System.Net.Sockets;
using System.Windows.Forms;

using Eneter.Messaging.EndPoints.StringMessages;
using Eneter.Messaging.MessagingSystems.MessagingSystemBase;
using Eneter.Messaging.MessagingSystems.TcpMessagingSystem;
using System.Collections.Generic;
using System.Diagnostics;

namespace SMSIM
{
    public partial class SMSIM : Form
    {

        private IDuplexStringMessageReceiver receiver;
        String connectedDevice;
        Dictionary<String, Conversation> openConversations = new Dictionary<string, Conversation>();

        public SMSIM()
        {
            InitializeComponent();
        }

        public class Contact
        {
            public String name { get; set; }
            public String number { get; set; }
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
            if (receiver.IsDuplexInputChannelAttached) ipAddress.Text = localIP;
            this.ActiveControl = label1;
            contacts.DisplayMember = "name";
            contacts.ValueMember = "number";
        }

        private void OnRequestReceived(object sender, StringRequestReceivedEventArgs e)
        {
            Console.WriteLine(e.RequestMessage);
            receiver.SendResponseMessage(e.ResponseReceiverId, "Ack: " + e.RequestMessage);
            String[] input = e.RequestMessage.Split(':');
            if (input[0].Equals("Conn"))
            {
                connectedDevice = e.ResponseReceiverId;
                deviceName.Invoke(new MethodInvoker(delegate { deviceName.Text = input[1]; }));
                contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Clear(); }));
            }
            if (input[0].Equals("DC"))
            {
                connectedDevice = null;
                deviceName.Invoke(new MethodInvoker(delegate { deviceName.Text = "-"; }));
                contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Clear(); }));
            }
            if (input[0].Equals("Contact"))
            {
                Contact contact = new Contact();
                contact.name = input[1];
                contact.number = input[2];
                contacts.Invoke(new MethodInvoker(delegate { contacts.Items.Add(contact); contacts.Sorted = true; }));
            }
            if (input[0].Equals("SMS"))
            {
                if (openConversations.ContainsKey(input[1]))
                {
                    Conversation conversation;
                    if (openConversations.TryGetValue(input[1], out conversation))
                    {
                        if (conversation.InvokeRequired) conversation.Invoke(new MethodInvoker(delegate { conversation.ParseInput(input); }));
                        else conversation.ParseInput(input);
                    }
                }
                else
                {
                    Conversation conversation = new Conversation(receiver, connectedDevice, input);
                    openConversations.Add(input[1], conversation);
                    conversation.Show();
                }
            }
        }

        private void contacts_doubleClick(object sender, MouseEventArgs e)
        {
            Contact selected = (Contact) contacts.SelectedItem;
            if (openConversations.ContainsKey(selected.name))
            {
                Conversation conversation;
                if (openConversations.TryGetValue(selected.name, out conversation))
                {
                    if (conversation.InvokeRequired) conversation.Invoke(new MethodInvoker(delegate { conversation.Focus(); }));
                    else conversation.Focus();
                }
            }
            else
            {
                String[] input = { "null", selected.name, selected.number };
                Conversation conversation = new Conversation(receiver, connectedDevice, input);
                openConversations.Add(selected.name, conversation);
                conversation.Show();
            }
        }

        private void SMSIM_Resize(object sender, EventArgs e)
        {
            if (FormWindowState.Minimized == this.WindowState)
            {
                trayIcon.Visible = true;
                this.Hide();
            }
        }

        private void trayIcon_MouseDoubleClick(object sender, MouseEventArgs e)
        {
            trayIcon.Visible = false;
            this.Show();
            this.WindowState = FormWindowState.Normal;
        }

        private void SMSIM_FormClosing(object sender, FormClosingEventArgs e)
        {
            receiver.DetachDuplexInputChannel();
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