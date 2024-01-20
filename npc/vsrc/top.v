module top(
  input clk,
  input rst,
  input sw_a,
  input sw_b,
  output sw_f
);
absw my_absw(
  .clk(clk),
  .rst(rst),
  .a(sw_a),
  .b(sw_b),
  .f(sw_f)
);
endmodule