module riscv_cpu(
    input clk,
    input rst,
    input [31:0] inst,
    input [31:0] mem_data,

    output [2:0] memop,
    output [31:0] memdata,
    output mem_wen,
    output [31:0] mem_addr
);

wire [31:0] nextPC;
wire [31:0] pc_out;

risc_V_pc pc (
    .clk(clk),
    .rst(rst),
    .pc_in(nextPC),
    .pc_out(pc_out)
);

wire [2:0] ExtOp;
wire RegWr;
wire [1:0] ALUAsrc;
wire [1:0] ALUBsrc;
wire [3:0] ALUctr;
wire [2:0] Branch;
wire MemtoReg;
wire [1:0] csr_ctr;

Contr_gen contr (
    .op(inst[6:0]),
    .func3(inst[14:12]),
    .func7(inst[31:25]),
    .priv(inst[31:20]),

    .ExtOp(ExtOp),
    .RegWr(RegWr),
    .ALUAsrc(ALUAsrc),
    .ALUBsrc(ALUBsrc),
    .ALUctr(ALUctr),
    .Branch(Branch),
    .MemtoReg(MemtoReg),
    .MemWr(mem_wen),
    .MemOp(memop),
    .csr_ctr(csr_ctr)
);

wire [31:0] rs1_val;
wire [31:0] rs2_val;
wire [31:0] sub_busW, busW;

risc_V_Reg_file reg_file (
    .clk(clk),
    .rst(rst),
    .waddr(inst[11:7]),
    .wdata(busW),
    .wen(RegWr),

    .raddra(inst[19:15]),
    .raddrb(inst[24:20]),
    .rdataa(rs1_val),
    .rdatab(rs2_val)
);

wire [31:0] imm;

ImmGen imm_get (
    .clock(clk),
    .reset(rst),
    .io_inst(inst),
    .io_extop(ExtOp),

    .io_imm(imm)
);

wire [31:0] csr_output;
wire [11:0] csr_raddr, csr_waddr1, csr_waddr2;
wire [31:0] csr_wdata1, csr_wdata2;

MuxKeyWithDefault #(2, 2, 12) csr_raddr_mux (csr_raddr, csr_ctr, imm[11:0], {
    2'b01, 12'h341,
    2'b11, 12'h305
});

MuxKeyWithDefault #(1, 2, 12) csr_waddr1_mux (csr_waddr1, csr_ctr, imm[11:0], {
    2'b11, 12'h341
});

MuxKeyWithDefault #(1, 2, 32) csr_wdata1_mux (csr_wdata1, csr_ctr, Result, {
    2'b11, pc_out
});
wire [31:0] csr_test;
assign csr_test = Result;

assign csr_waddr2 = 12'h342;
assign csr_wdata2 = 32'd11;

riscv_V_csr csr (
    .test(Result),
    .clk(clk),
    .rst(rst),
    .csr_raddr(csr_raddr),
    .csr_waddr1(csr_waddr1),
    .csr_wdata1(csr_wdata1),
    .csr_waddr2(csr_waddr2),
    .csr_wdata2(csr_wdata2),
    .csr_ctr(csr_ctr),
    .csr_output(csr_output)
);

wire [31:0] alu_srcA;
wire [31:0] alu_srcB;

MuxKeyWithDefault #(3, 2, 32) alu_srcA_mux (alu_srcA, ALUAsrc, 32'h00000000, {
    2'b00, rs1_val,
    2'b01, pc_out,
    2'b10, csr_output
});

MuxKeyWithDefault #(4, 2, 32) alu_srcB_mux (alu_srcB, ALUBsrc, 32'h00000000, {
    2'b00, rs2_val,
    2'b01, imm,
    2'b10, 32'd4,
    2'b11, rs1_val
});

wire Less;
wire Zero;
wire [31:0] Result;

ALU alu (
    .clock(clk),
    .reset(rst),

    .io_ALUctr(ALUctr),
    .io_src_A(alu_srcA),
    .io_src_B(alu_srcB),

    .io_ALUout(Result),
    .io_Zero(Zero),
    .io_Less(Less)
);

wire PCAsrc;
wire PCBsrc;

BranchCond branch (
    .clock(clk),
    .reset(rst),

    .io_Branch(Branch),
    .io_Zero(Zero),
    .io_Less(Less),
    .io_PCAsrc(PCAsrc),
    .io_PCBsrc(PCBsrc)
);

wire [31:0] PCA_val;
wire [31:0] PCB_val;

MuxKeyWithDefault #(2, 1, 32) PCA_mux (PCA_val, PCAsrc, 32'h00000000, {
    1'b0, 32'd4,
    1'b1, imm
});

MuxKeyWithDefault #(2, 1, 32) PCB_mux (PCB_val, PCBsrc, 32'h00000000, {
    1'b0, pc_out,
    1'b1, rs1_val
});

wire [31:0] added_pc;
assign added_pc = (PCB_val + PCA_val);
MuxKeyWithDefault #(2, 2, 32) nextPC_mux (nextPC, csr_ctr, added_pc, {
    2'b01, csr_output,
    2'b11, csr_output
});

MuxKeyWithDefault #(2, 1, 32) mem_to_Reg_mux (sub_busW, MemtoReg, 32'h00000000, {
    1'b0, Result,
    1'b1, mem_data
});

MuxKeyWithDefault #(1, 2, 32) busW_mux (busW, csr_ctr, sub_busW, {
    2'b10, csr_output
});

assign mem_addr = Result;
assign memdata = rs2_val;

endmodule