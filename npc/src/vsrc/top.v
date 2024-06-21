import "DPI-C" function int npc_trap (input int ra);

module top(
    input clk,
    input rst,
    output [31:0] Imem_raddr,
    input [31:0] Imem_rdata,

    input [31:0] Dmem_data,

    output [2:0] Dmemop,
    output [31:0] Dmemdata,
    output Dmem_wen,
    output [31:0] Dmem_addr
);

always @(Imem_rdata) begin
    if(Imem_rdata == 32'h00100073)
        $display("sim has been stop at clk_cnt %d", npc_trap(npc.riscv_cpu.REG.gpr_10));
end


npc npc (
    .clock(clk),
    .reset(rst),
    .io_Imem_rdata(Imem_rdata),
    .io_Imem_raddr(Imem_raddr),

    .io_Dmem_rdata(Dmem_data),
    .io_Dmem_wdata(Dmemdata),
    .io_Dmem_wop(Dmemop),
    .io_Dmem_wen(Dmem_wen),

    .io_Dmem_wraddr(Dmem_addr)
);

endmodule