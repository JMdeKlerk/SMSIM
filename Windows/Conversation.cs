using Eneter.Messaging.EndPoints.StringMessages;
using System;
using System.Windows.Forms;

namespace SMSIM
{
    public partial class Conversation : Form
    {

        private IDuplexStringMessageReceiver receiver;
        private String device, name, number;

        public Conversation(IDuplexStringMessageReceiver receiver, String connectedDevice, string[] input)
        {
            InitializeComponent();
            this.receiver = receiver;
            this.device = connectedDevice;
            this.name = input[1];
            this.number = input[2];
            ParseInput(input);
        }

        public void ParseInput(string[] input)
        {
            this.Text = input[1] + " (" + input[2] + ")";
            if (input.Length > 3)
            {
                string timestamp = "[" + DateTime.Now.ToString("HH:mm:ss") + "] ";
                messageBox.AppendText(timestamp + input[1] + ": " + input[3] + "\n");
            }
        }

        private void sendMessage(String message)
        {
            receiver.SendResponseMessage(device, "SMS:" + this.number + ":" + message);
            string timestamp = "[" + DateTime.Now.ToString("HH:mm:ss") + "] ";
            messageBox.AppendText(timestamp + "You: " + message + "\n");
        }

        private void send_Click(object sender, EventArgs e)
        {
            sendMessage(entry.Text);
            entry.Text = "";
        }

        private void entry_KeyPress(object sender, KeyPressEventArgs e)
        {
            if (e.KeyChar == (char)Keys.Enter)
            {
                sendMessage(entry.Text);
                entry.Text = "";
            }
        }
    }
}
