﻿namespace SMSIM
{
    partial class SMSIM
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.label1 = new System.Windows.Forms.Label();
            this.deviceName = new System.Windows.Forms.TextBox();
            this.ipAddress = new System.Windows.Forms.TextBox();
            this.label2 = new System.Windows.Forms.Label();
            this.SuspendLayout();
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(12, 41);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(44, 13);
            this.label1.TabIndex = 0;
            this.label1.Text = "Device:";
            // 
            // deviceName
            // 
            this.deviceName.Location = new System.Drawing.Point(68, 38);
            this.deviceName.Name = "deviceName";
            this.deviceName.ReadOnly = true;
            this.deviceName.Size = new System.Drawing.Size(138, 20);
            this.deviceName.TabIndex = 1;
            this.deviceName.Text = "-";
            this.deviceName.TextAlign = System.Windows.Forms.HorizontalAlignment.Center;
            // 
            // ipAddress
            // 
            this.ipAddress.Location = new System.Drawing.Point(68, 12);
            this.ipAddress.Name = "ipAddress";
            this.ipAddress.ReadOnly = true;
            this.ipAddress.Size = new System.Drawing.Size(138, 20);
            this.ipAddress.TabIndex = 2;
            this.ipAddress.Text = "-";
            this.ipAddress.TextAlign = System.Windows.Forms.HorizontalAlignment.Center;
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Location = new System.Drawing.Point(12, 15);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(48, 13);
            this.label2.TabIndex = 3;
            this.label2.Text = "Address:";
            // 
            // SMSIM
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(218, 261);
            this.Controls.Add(this.label2);
            this.Controls.Add(this.ipAddress);
            this.Controls.Add(this.deviceName);
            this.Controls.Add(this.label1);
            this.Name = "SMSIM";
            this.Text = "SMSIM";
            this.Load += new System.EventHandler(this.SMSIM_Load);
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.TextBox deviceName;
        private System.Windows.Forms.TextBox ipAddress;
        private System.Windows.Forms.Label label2;
    }
}

