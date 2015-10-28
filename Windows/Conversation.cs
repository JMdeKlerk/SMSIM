using System;
using System.Windows.Forms;

namespace SMSIM
{
    public partial class Conversation : Form
    {
        
        private SMSIM parent;
        private String name, number;

        public Conversation(SMSIM parent, string[] input)
        {
            InitializeComponent();
            this.parent = parent;
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
                messageBox.AppendText(timestamp + input[1] + ": ");
                for (int i = 3; i < input.Length; i++)
                {
                    messageBox.AppendText(input[i]);
                    if (i < input.Length - 1) messageBox.AppendText(":");
                }
                messageBox.AppendText("\n");
                messageBox.ScrollToCaret();
            }
        }

        private void sendMessage(String message)
        {
            parent.sendMessage("SMS:" + this.number + ":" + message);
            string timestamp = "[" + DateTime.Now.ToString("HH:mm:ss") + "] ";
            messageBox.AppendText(timestamp + "You: " + message + "\n");
            messageBox.ScrollToCaret();
        }

        private void Conversation_FormClosing(object sender, FormClosingEventArgs e)
        {
            parent.openConversations.Remove(this.name);
        }

        private void send_Click(object sender, EventArgs e)
        {
            sendMessage(entry.Text);
            entry.Text = "";
        }

    }

}
