using System;
using System.Collections;
using System.Drawing;
using System.Threading;
using System.Windows.Forms;

namespace SMSIM {
    public partial class Conversation : Form {

        private SMSIM parent;
        private String name, number;
        private ArrayList pendingMessages = new ArrayList();

        private class PendingMessage {
            public int id { get; set; }
            public int startIndex { get; set; }
            public int length { get; set; }
        }

        public Conversation(SMSIM parent, string[] input) {
            InitializeComponent();
            this.parent = parent;
            this.name = input[2];
            this.number = input[3];
            ParseInput(input);
        }

        public void ParseInput(string[] input) {
            this.Text = input[2] + " (" + input[3] + ")";
            if (input.Length > 4) {
                string timestamp = "[" + DateTime.Now.ToString("HH:mm:ss") + "] ";
                messageBox.AppendText(timestamp + input[2] + ": ");
                for (int i = 4; i < input.Length; i++) {
                    messageBox.AppendText(input[i]);
                    if (i < input.Length - 1) messageBox.AppendText(":");
                }
                messageBox.AppendText("\n");
                messageBox.ScrollToCaret();
            }
        }

        private void sendMessage(String message) {
            int id = Interlocked.Increment(ref SMSIM.smsId);
            string timestamp = "[" + DateTime.Now.ToString("HH:mm:ss") + "] ";
            int startIndex = messageBox.Text.Length;
            int length = (timestamp + "You: " + message + "\n").Length;
            this.Invoke(new MethodInvoker(delegate {
                messageBox.AppendText(timestamp + "You: " + message + "\n");
                messageBox.ScrollToCaret();
                messageBox.Select(startIndex, length);
                messageBox.SelectionFont = new System.Drawing.Font("Sans Serif", 8, FontStyle.Italic);
            }));
            PendingMessage pendingMessage = new PendingMessage();
            pendingMessage.id = id;
            pendingMessage.length = length;
            pendingMessage.startIndex = startIndex;
            pendingMessages.Add(pendingMessage);
            parent.sendMessage("SMS:" + id + ":" + this.number + ":" + message);
        }

        public void messageSuccess(int id) {
            foreach (PendingMessage pendingMessage in pendingMessages) {
                if (pendingMessage.id == id) {
                    this.Invoke(new MethodInvoker(delegate {
                        messageBox.Select(pendingMessage.startIndex, pendingMessage.length);
                        messageBox.SelectionFont = new System.Drawing.Font("Sans Serif", 8);
                    }));
                    pendingMessages.Remove(pendingMessage);
                    break;
                }
            }
        }

        public void messageFail(int id) {
            foreach (PendingMessage pendingMessage in pendingMessages) {
                if (pendingMessage.id == id) {
                    this.Invoke(new MethodInvoker(delegate {
                        messageBox.Select(pendingMessage.startIndex, pendingMessage.length);
                        messageBox.SelectionFont = new System.Drawing.Font("Sans Serif", 8);
                        messageBox.SelectionColor = Color.Red;
                    }));
                    pendingMessages.Remove(pendingMessage);
                    break;
                }
            }
        }

        private void Conversation_FormClosing(object sender, FormClosingEventArgs e) {
            parent.openConversations.Remove(this.name);
        }

        private void send_Click(object sender, EventArgs e) {
            sendMessage(entry.Text);
            entry.Text = "";
        }

    }

}
