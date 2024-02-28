import "DPI-C" function int sim_stop (input int ra);

module top(
    input clk,
    input rst,
    input [31:0] inst,

    input [31:0] mem_data,

    output [2:0] memop,
    output [31:0] memdata,
    output mem_wen,
    output [31:0] mem_addr,

    output [15:0] test,
    output [31:0] test1,
    output [31:0] pc
);

always @(inst) begin
    if(inst == 32'h00100073)
        $display("sim has been stop at clk_cnt %d", sim_stop(cpu.reg_file.rf.rf[10]));
end

assign pc = cpu.pc_out;

riscv_cpu cpu (
    .clk(clk),
    .rst(rst),
    .inst(inst),
    .mem_data(mem_data),

    .memop(memop),
    .memdata(memdata),
    .mem_wen(mem_wen),
    .mem_addr(mem_addr)
);

assign test = cpu.Result[15:0];
assign test1 = cpu.Result;

endmodule