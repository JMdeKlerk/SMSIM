using System;
using System.Drawing;
using System.Windows.Forms;

namespace SMSIM
{
    public class FancyListBox : ListBox
    {
        public FancyListBox()
        {
            DrawMode = DrawMode.OwnerDrawFixed;
            ItemHeight = 54;
        }

        protected override void OnDrawItem(DrawItemEventArgs e)
        {
            const TextFormatFlags flags = TextFormatFlags.Left | TextFormatFlags.VerticalCenter;

            if (e.Index >= 0)
            {
                e.DrawBackground();
                e.Graphics.DrawRectangle(Pens.Red, 2, e.Bounds.Y + 5, 42, 42); // Pretend display picture

                var textRect = e.Bounds;
                textRect.X += 50;
                textRect.Width -= 50;
                textRect.Y -= 10;
                //string itemText = DesignMode ? "Contacts" : Items[e.Index].ToString();
                SMSIM.Contact contact = (SMSIM.Contact)Items[e.Index];
                string itemText = contact.name;
                TextRenderer.DrawText(e.Graphics, itemText, e.Font, textRect, e.ForeColor, flags);
                textRect.Y += 15;
                itemText = contact.number;
                TextRenderer.DrawText(e.Graphics, itemText, e.Font, textRect, e.ForeColor, flags);
                e.DrawFocusRectangle();
            }
        }
    }
}
