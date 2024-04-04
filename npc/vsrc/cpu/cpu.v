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
wire ALUAsrc;
wire [1:0] ALUBsrc;
wire [3:0] ALUctr;
wire [2:0] Branch;
wire MemtoReg;

Contr_gen contr (
    .op(inst[6:0]),
    .func3(inst[14:12]),
    .func7(inst[31:25]),

    .ExtOp(ExtOp),
    .RegWr(RegWr),
    .ALUAsrc(ALUAsrc),
    .ALUBsrc(ALUBsrc),
    .ALUctr(ALUctr),
    .Branch(Branch),
    .MemtoReg(MemtoReg),
    .MemWr(mem_wen),
    .MemOp(memop)
);

wire [31:0] rs1_val;
wire [31:0] rs2_val;
wire [31:0] busW;

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

imm_gen imm_get (
    .inst(inst),
    .extop(ExtOp),

    .imm(imm)
);

wire [31:0] alu_srcA;
wire [31:0] alu_srcB;

MuxKeyWithDefault #(2, 1, 32) alu_srcA_mux (alu_srcA, ALUAsrc, 32'h00000000, {
    1'b0, rs1_val,
    1'b1, pc_out
});

MuxKeyWithDefault #(3, 2, 32) alu_srcB_mux (alu_srcB, ALUBsrc, 32'h00000000, {
    2'b00, rs2_val,
    2'b01, imm,
    2'b10, 32'd4
});

wire Less;
wire Zero;
wire [31:0] Result;

ALU alu (
    .ALUctr(ALUctr),
    .A(alu_srcA),
    .B(alu_srcB),

    .ALUout(Result),
    .Zero(Zero),
    .Less(Less)
);

wire PCAsrc;
wire PCBsrc;

branch_cond branch (
    .Branch(Branch),
    .Zero(Zero),
    .Less(Less),
    .PCAsrc(PCAsrc),
    .PCBsrc(PCBsrc)
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

assign nextPC = (PCB_val + PCA_val);

MuxKeyWithDefault #(2, 1, 32) mem_to_Reg_mux (busW, MemtoReg, 32'h00000000, {
    1'b0, Result,
    1'b1, mem_data
});

assign mem_addr = Result;
assign memdata = rs2_val;

endmodule