using System;
using System.Windows.Forms;

namespace SMSIM
{
    public partial class Conversation : Form
    {
        
        public Conversation(string[] input)
        {
            InitializeComponent();
            ParseInput(input);
        }

        public void ParseInput(string[] input)
        {
            this.Text = input[1] + " (" + input[2] + ")";
            if (input.Length > 3)
            {
                string timestamp = "[" + DateTime.Now.ToString("HH:mm:ss") + "] ";
                richTextBox1.AppendText(timestamp + input[1] + ": " + input[3] + "\n");
            }
        }
    }
}
