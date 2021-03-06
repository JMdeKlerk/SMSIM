﻿using System;
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
                SMSIM.Contact contact = (SMSIM.Contact)Items[e.Index];
                e.Graphics.DrawImage(contact.displayPic, 5, e.Bounds.Y + 5, 42, 42);
                var textRect = e.Bounds;
                textRect.X += 50;
                textRect.Width -= 50;
                textRect.Y -= 10;
                TextRenderer.DrawText(e.Graphics, contact.name, e.Font, textRect, e.ForeColor, flags);
                textRect.Y += 15;
                TextRenderer.DrawText(e.Graphics, contact.number, e.Font, textRect, e.ForeColor, flags);
                e.DrawFocusRectangle();
            }
        }
        
        protected override void Sort()
        {
            if (Items.Count > 1)
            {
                bool swapped = true;
                while (swapped == true)
                {
                    int counter = Items.Count - 1;
                    swapped = false;
                    while (counter > 0)
                    {
                        SMSIM.Contact contact = (SMSIM.Contact)Items[counter];
                        SMSIM.Contact compare = (SMSIM.Contact)Items[counter - 1];
                        if (contact.name.CompareTo(compare.name) == -1)
                        {
                            object temp = Items[counter];
                            Items[counter] = Items[counter - 1];
                            Items[counter - 1] = temp;
                            swapped = true;
                        }
                        counter -= 1;
                    }
                }
            }
        }

    }
}
