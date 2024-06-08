module Debouncer(	 
  input  clock,	 
         reset,	 
         io_input,	 
  output io_output	 
);

  reg [13:0] count;	 
  reg        Iv;	 
  reg        Output_filiter;	 
  always @(posedge clock) begin	 
    if (reset) begin	 
      count <= 14'h0;	 
      Iv <= 1'h0;	 
      Output_filiter <= 1'h0;	 
    end
    else begin	 
      automatic logic _GEN;	 
      _GEN = io_input == Iv;	 
      if (_GEN)	 
        count <= count + 14'h1;	 
      else	 
        count <= 14'h0;	 
      Iv <= io_input;	 
      if (_GEN & count == 14'h270F)	 
        Output_filiter <= io_input;	 
    end
  end  
    
  assign io_output = Output_filiter;	 
endmodule

module key(	 
  input  clock,	 
         reset,	 
         io_key_in,	 
  output io_is_key_posedge	 
);

  wire _key_in_debouncer_io_output;	 
  reg  key_in_pre;	 
  always @(posedge clock) begin	 
    if (reset)	 
      key_in_pre <= 1'h1;	 
    else	 
      key_in_pre <= _key_in_debouncer_io_output;	 
  end  
   	 
  Debouncer key_in_debouncer (	 
    .clock     (clock),
    .reset     (reset),
    .io_input  (io_key_in),
    .io_output (_key_in_debouncer_io_output)
  );	 
  assign io_is_key_posedge = ~key_in_pre & _key_in_debouncer_io_output;	 
endmodule

module Timer(	 
  input         clock,	 
                reset,	 
  output [31:0] io_time_10m_seconds,	 
  input         io_clear,	 
                io_stop,	 
                io_up_or_down	 
);

  reg [31:0] timer_counter;	 
  reg [31:0] total_10m_seconds;	 
  always @(posedge clock) begin	 
    if (reset) begin	 
      timer_counter <= 32'h0;	 
      total_10m_seconds <= 32'h0;	 
    end
    else if (timer_counter == 32'hFFFFF) begin	 
      timer_counter <= 32'h0;	 
      if (io_clear) begin	 
        if (io_stop) begin	 
          if (io_up_or_down)	 
            total_10m_seconds <= total_10m_seconds + 32'h1;	 
          else if (total_10m_seconds == 32'h0)	 
            total_10m_seconds <= 32'h0;	 
          else	 
            total_10m_seconds <= total_10m_seconds - 32'h1;	 
        end
      end
      else	 
        total_10m_seconds <= 32'h0;	 
    end
    else	 
      timer_counter <= timer_counter + 32'h1;	 
  end  
   	 
  assign io_time_10m_seconds = total_10m_seconds;	 
endmodule

module BCDDecoder(	 
  input  [3:0] io_in,	 
  output [7:0] io_out	 
);

  wire [6:0]       _GEN = {1'h0, io_in == 4'h1 ? 6'h6 : 6'h3F};	 
  wire [15:0][6:0] _GEN_0 =
    {{_GEN},
     {_GEN},
     {_GEN},
     {_GEN},
     {7'h7F},
     {7'h77},
     {7'h6F},
     {7'h7F},
     {7'h7},
     {7'h7D},
     {7'h6D},
     {7'h66},
     {7'h4F},
     {7'h5B},
     {_GEN},
     {_GEN}};	 
  assign io_out =
    (&io_in)
      ? 8'h8E
      : io_in == 4'hE
          ? 8'h9E
          : io_in == 4'hD ? 8'h7A : io_in == 4'hC ? 8'h9D : {1'h0, _GEN_0[io_in]};	 
endmodule

module Homework(	 
  input        clock,	 
               reset,	 
               io_sw1,	 
               io_clear,	 
               io_stop,	 
               io_up_or_down,	 
  output [7:0] io_out,	 
  output [3:0] io_bit	 
);

  wire [7:0]  _decoder4_io_out;	 
  wire [7:0]  _decoder3_io_out;	 
  wire [7:0]  _decoder2_io_out;	 
  wire [7:0]  _decoder1_io_out;	 
  wire [31:0] _timer_io_time_10m_seconds;	 
  wire        _key_1_io_is_key_posedge;	 
  reg  [1:0]  state;	 
  wire [31:0] total_seconds = _timer_io_time_10m_seconds / 32'h64;	 
  wire [31:0] _time_type_Choice_T = total_seconds % 32'h3C;	 
  wire [31:0] _time_type_Choice_T_1 = _timer_io_time_10m_seconds % 32'h63;	 
  wire [31:0] time_type_Choice =
    state == 2'h2
      ? {25'h0, _time_type_Choice_T_1[6:0]}
      : state == 2'h1
          ? total_seconds / 32'h3C
          : (|state) ? total_seconds : {26'h0, _time_type_Choice_T[5:0]};	 
  wire [31:0] _decoder1_io_in_T_1 = time_type_Choice % 32'hA;	 
  wire [31:0] _decoder2_io_in_T_2 = time_type_Choice / 32'hA % 32'hA;	 
  wire [31:0] _decoder3_io_in_T_2 = time_type_Choice / 32'h64 % 32'hA;	 
  wire [31:0] _decoder4_io_in_T_2 = time_type_Choice / 32'h3E8 % 32'hA;	 
  reg  [3:0]  bit_reg;	 
  reg  [31:0] counter;	 
  always @(posedge clock) begin	 
    if (reset) begin	 
      state <= 2'h0;	 
      bit_reg <= 4'hE;	 
      counter <= 32'h0;	 
    end
    else begin	 
      if (_key_1_io_is_key_posedge) begin	 
        if (state == 2'h2)	 
          state <= 2'h0;	 
        else if (state == 2'h1)	 
          state <= 2'h2;	 
        else	 
          state <= {1'h0, ~(|state)};	 
      end
      if (counter == 32'h3E8) begin	 
        bit_reg <= {bit_reg[2:0], bit_reg[3]};	 
        counter <= 32'h0;	 
      end
      else	 
        counter <= counter + 32'h1;	 
    end
  end  
   	 
     	
  key key_1 (	 
    .clock             (clock),
    .reset             (reset),
    .io_key_in         (io_sw1),
    .io_is_key_posedge (_key_1_io_is_key_posedge)
  );	 
  Timer timer (	 
    .clock               (clock),
    .reset               (reset),
    .io_time_10m_seconds (_timer_io_time_10m_seconds),
    .io_clear            (io_clear),
    .io_stop             (io_stop),
    .io_up_or_down       (io_up_or_down)
  );	 
  BCDDecoder decoder1 (	 
    .io_in  (_decoder1_io_in_T_1[3:0]),	 
    .io_out (_decoder1_io_out)
  );	 
  BCDDecoder decoder2 (	 
    .io_in  (_decoder2_io_in_T_2[3:0]),	 
    .io_out (_decoder2_io_out)
  );	 
  BCDDecoder decoder3 (	 
    .io_in  (_decoder3_io_in_T_2[3:0]),	 
    .io_out (_decoder3_io_out)
  );	 
  BCDDecoder decoder4 (	 
    .io_in  (_decoder4_io_in_T_2[3:0]),	 
    .io_out (_decoder4_io_out)
  );	 
  assign io_out =
    bit_reg == 4'h7
      ? _decoder4_io_out
      : bit_reg == 4'hB
          ? _decoder3_io_out
          : bit_reg == 4'hD
              ? _decoder2_io_out
              : bit_reg == 4'hE ? _decoder1_io_out : 8'h0;	 
  assign io_bit = bit_reg;	 
endmodule
