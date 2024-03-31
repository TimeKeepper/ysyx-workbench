module RegisterFile #(ADDR_WIDTH = 1, DATA_WIDTH = 1) (
    input clk,
    input rst,
    input [DATA_WIDTH-1:0] wdata,
    input [ADDR_WIDTH-1:0] waddr,
    input wen,

    input [ADDR_WIDTH-1:0] raddra,
    input [ADDR_WIDTH-1:0] raddrb,//需要能够同时读两个寄存器并且写一个寄存器
    output [DATA_WIDTH-1:0] rdataa,
    output [DATA_WIDTH-1:0] rdatab
);
    reg [DATA_WIDTH-1:0] rf [2**ADDR_WIDTH-1:0];
    always @(posedge clk) begin
        if (wen && (waddr != 5'b00000)) rf[waddr] <= wdata;
        if (rst) for (int i = 0; i < 2**ADDR_WIDTH; i = i + 1) rf[i] <= 0;
    end

    assign rdataa = rf[raddra];
    assign rdatab = rf[raddrb];
endmodule

module risc_V_Reg_file(
    input clk,
    input rst,
    input [4:0] waddr,
    input [31:0] wdata,
    input wen,

    input [4:0] raddra,
    input [4:0] raddrb,

    output [31:0] rdataa,
    output [31:0] rdatab
);

    RegisterFile #(5, 32) rf (
        .clk(clk),
        .rst(rst),
        .wdata(wdata),
        .waddr(waddr),
        .wen(wen),

        .raddra(raddra),
        .raddrb(raddrb),
        .rdataa(rdataa),
        .rdatab(rdatab)
    );
  
    assign rf.rf[0] = 32'h00000000;

endmodule

module risc_V_pc(
    input clk,
    input rst,
    input [31:0] pc_in,
    output [31:0] pc_out
);

    reg [31:0] pc;

    always @(posedge clk or posedge rst) begin
        if (rst) pc <= 32'h80000000;
        else pc <= pc_in;
    end

    assign pc_out = pc;

endmodule

module riscv_V_csr (
    input      clk,
    input      rst,
    input      csr_raddr,
    input      csr_waddr1,
    input      csr_wdata1,
    input      csr_waddr2,
    input      csr_wdata2,
    input      csr_ctl,

    output  reg   csr_output
);

reg [31:0] mstatus, mtvec, mscratch, mepc, mcause;

always @(posedge clk or posedge rst) begin
    if (rst) begin
        mstatus <= 32'h00000000;
        mtvec <= 32'h00000000;
        mscratch <= 32'h00000000;
        mepc <= 32'h00000000;
        mcause <= 32'h00000000;
    end
    if(csr_ctl != 2'b00) begin
        case(csr_raddr)
            5'b00000: csr_output <= mstatus;
            5'b00101: csr_output <= mtvec;
            5'b00110: csr_output <= mscratch;
            5'b01101: csr_output <= mepc;
            5'b11101: csr_output <= mcause;
        endcase
    end
    if(csr_ctl[0] == 1'b1) begin
        case(csr_waddr1)
            5'b00000: mstatus <= csr_wdata1;
            5'b00101: mtvec <= csr_wdata1;
            5'b00110: mscratch <= csr_wdata1;
            5'b01101: mepc <= csr_wdata1;
            5'b11101: mcause <= csr_wdata1;
        endcase
    end
    if(csr_ctl == 2'b11) begin
        case(csr_waddr2)
            5'b00000: mstatus <= csr_wdata2;
            5'b00101: mtvec <= csr_wdata2;
            5'b00110: mscratch <= csr_wdata2;
            5'b01101: mepc <= csr_wdata2;
            5'b11101: mcause <= csr_wdata2;
        endcase
    end
end
    
endmodule //riscv_V_csr

