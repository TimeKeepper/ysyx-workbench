module top(
  input clk,
  input rst,
  input sw_a,
  input sw_b,
  output sw_f
);
  assign sw_f = sw_a ^ sw_b;
  
endmodule