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
                richTextBox1.AppendText(input[1] + ": " + input[3]);
            }
        }
    }
}
