import "DPI-C" function int npc_trap (input int ra);

module top(
    input clk,
    input rst,
    input         io_AXI_araddr_ready,	
    output        io_AXI_araddr_valid,
    output [31:0] io_AXI_araddr_bits_addr,
    output        io_AXI_raddr_ready,	
    input         io_AXI_raddr_valid,	
    input  [31:0] io_AXI_raddr_bits_data,	
    input         io_AXI_raddr_bits_resp,	

    input [31:0] Dmem_data,

    output [2:0] Dmemop,
    output [31:0] Dmemdata,
    output Dmem_wen,
    output [31:0] Dmem_addr,

    output inst_comp
);

always @(io_AXI_raddr_bits_data) begin
    if(io_AXI_raddr_bits_data == 32'h00100073)
        $display("sim has been stop at clk_cnt %d", npc_trap(npc.CPU.REG.gpr_10));
end


npc npc (
    .clock(clk),
    .reset(rst),
    .io_AXI_araddr_ready(io_AXI_araddr_ready),
    .io_AXI_araddr_valid(io_AXI_araddr_valid),
    .io_AXI_araddr_bits_addr(io_AXI_araddr_bits_addr),
    .io_AXI_raddr_ready(io_AXI_raddr_ready),
    .io_AXI_raddr_valid(io_AXI_raddr_valid),
    .io_AXI_raddr_bits_data(io_AXI_raddr_bits_data),
    .io_AXI_raddr_bits_resp(io_AXI_raddr_bits_resp),

    .io_Dmem_rdata(Dmem_data),
    .io_Dmem_wdata(Dmemdata),
    .io_Dmem_wop(Dmemop),
    .io_Dmem_wen(Dmem_wen),

    .io_Dmem_wraddr(Dmem_addr),

    .io_inst_comp(inst_comp)
);

endmodule