module Debouncer_ps2(	 
  input  clock,	 
         reset,	 
         io_input,	 
  output io_output	 
);

  reg [4:0] count;	 
  reg       Iv;	 
  reg       Output_filiter;	 
  always @(posedge clock) begin	 
    if (reset) begin	 
      count <= 5'h0;	 
      Iv <= 1'h0;	 
      Output_filiter <= 1'h0;	 
    end
    else begin	 
      automatic logic _GEN;	 
      _GEN = io_input == Iv;	 
      if (_GEN)	 
        count <= count + 5'h1;	 
      else	 
        count <= 5'h0;	 
      Iv <= io_input;	 
      if (_GEN & count == 5'h13)	 
        Output_filiter <= io_input;	 
    end
  end  
  assign io_output = Output_filiter;	 
endmodule

module PS2Receiver(	 
  input        clock,	 
               reset,	 
               io_kdata,	 
  output       io_keycode_valid,	 
  output [7:0] io_keycode_bits	 
);

  wire       _kdata_filiter_io_output;	 
  reg  [7:0] data_cur;	 
  reg        flag_prev;	 
  reg  [3:0] cnt;	 
  wire       flag_cur = cnt == 4'hA;	 
  always @(posedge clock) begin	 
    if (reset) begin	 
      data_cur <= 8'h0;	 
      flag_prev <= 1'h0;	 
      cnt <= 4'h0;	 
    end
    else begin	 
      data_cur <= {data_cur[6:0], _kdata_filiter_io_output};	 
      flag_prev <= flag_cur;	 
      if (cnt == 4'hB)	 
        cnt <= 4'h0;	 
      else	 
        cnt <= cnt + 4'h1;	 
    end
  end  
   	 
  Debouncer_ps2 kdata_filiter (	 
    .clock     (clock),
    .reset     (reset),
    .io_input  (io_kdata),
    .io_output (_kdata_filiter_io_output)
  );	 
  assign io_keycode_valid = ~flag_prev & flag_cur;	 
  assign io_keycode_bits = data_cur;	 
endmodule

module Mouse_Ps2_Controller(	 
  input  clock,	 
         reset,	 
         io_kclk,	 
         io_kdata,	 
  output io_mouse_left_click	 
);

  wire       _ps2_receiver_io_keycode_valid;	 
  wire [7:0] _ps2_receiver_io_keycode_bits;	 
  reg        state;	 
  reg  [7:0] cnt;	 
  always @(posedge clock) begin	 
    if (reset) begin	 
      state <= 1'h0;	 
      cnt <= 8'h0;	 
    end
    else begin	 
      automatic logic _GEN;	 
      _GEN = cnt == 8'h3;	 
      state <= ~state & _GEN | state;	 
      if (state) begin	 
        if (cnt == 8'h4)	 
          cnt <= 8'h0;	 
        else if (_ps2_receiver_io_keycode_valid)	 
          cnt <= cnt + 8'h1;	 
      end
      else if (_GEN)	 
        cnt <= 8'h0;	 
      else if (_ps2_receiver_io_keycode_valid)	 
        cnt <= cnt + 8'h1;	 
    end
  end  
   	 
  PS2Receiver ps2_receiver (	 
    .clock            (clock),
    .reset            (reset),
    .io_kdata         (io_kdata),
    .io_keycode_valid (_ps2_receiver_io_keycode_valid),
    .io_keycode_bits  (_ps2_receiver_io_keycode_bits)
  );	 
  assign io_mouse_left_click = cnt == 8'h1 & state & _ps2_receiver_io_keycode_bits[7];	 
endmodule
