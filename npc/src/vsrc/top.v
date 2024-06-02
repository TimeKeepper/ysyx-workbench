import "DPI-C" function int npc_trap (input int ra);

module top(
    input clk,
    input rst,
    input [31:0] inst,

    input [31:0] mem_data,

    output [2:0] memop,
    output [31:0] memdata,
    output mem_wen,
    output [31:0] mem_addr
);

always @(inst) begin
    if(inst == 32'h00100073)
        $display("sim has been stop at clk_cnt %d", npc_trap(npc.riscv_cpu.REG.gpr_10));
end

npc npc (
    .clock(clk),
    .reset(rst),
    .io_inst(inst),
    .io_mem_rdata(mem_data),

    .io_mem_wop(memop),
    .io_mem_wdata(memdata),
    .io_mem_wen(mem_wen),
    .io_mem_raddr(mem_addr)
);

endmodule