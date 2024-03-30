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

module risc_V_csr (
    input      clk,
    input      rst,
    
);
    
endmodule //risc_V_csr

