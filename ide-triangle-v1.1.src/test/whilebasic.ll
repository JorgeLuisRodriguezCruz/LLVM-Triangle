@last_char = global i32 0

; Format strings
@.str.int = private unnamed_addr constant [4 x i8] c"%d\0A\00", align 1
@.str.int.scanf = private unnamed_addr constant [3 x i8] c"%d\00", align 1
@.str.char = private unnamed_addr constant [3 x i8] c"%c\00", align 1
@.str.bool = private unnamed_addr constant [4 x i8] c"%s\0A\00", align 1
@.str.true = private unnamed_addr constant [5 x i8] c"true\00", align 1
@.str.false = private unnamed_addr constant [6 x i8] c"false\00", align 1

; Standard Library Functions
declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
declare i32 @putchar(i32)
declare i32 @getchar()

define i1 @eol() {
entry:
  %last = load i32, i32* @last_char
  %cmp = icmp eq i32 %last, 10  ; '\n'
  br i1 %cmp, label %true, label %check_eof
  check_eof:
  %cmp2 = icmp eq i32 %last, -1  ; EOF
  br i1 %cmp2, label %true, label %false
  true:
  ret i1 true
  false:
  ret i1 false
}

define void @geteol() {
entry:
  %ch = call i32 @getchar()
  store i32 %ch, i32* @last_char
  ret void
}

define void @puteol() {
entry:
  %result = call i32 @putchar(i32 10)  ; '\n'
  ret void
}

define void @getint(i32* %ptr) {
entry:
  %result = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str.int.scanf, i32 0, i32 0), i32* %ptr)
  ret void
}

define i32 @main() {
entry:
  %flag = alloca i1
  %ch = alloca i8
  store i1 true, i1* %flag
  br label %label0
  label0:
  %temp0 = load i1, i1* %flag
  br i1 %temp0, label %label1, label %label2
  label1:
  %temp1 = call i32 @getchar()
  %temp2 = trunc i32 %temp1 to i8
  store i8 %temp2, i8* %ch
  %temp3 = load i8, i8* %ch
  %temp5 = sext i8 %temp3 to i32
  call i32 @putchar(i32 %temp5)
  call i32 @putchar(i32 32)
  br label %label0
  label2:
  ret i32 0
}
